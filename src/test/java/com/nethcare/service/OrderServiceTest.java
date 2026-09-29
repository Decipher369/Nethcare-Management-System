package com.nethcare.service;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.Order;
import com.nethcare.model.OrderItem;
import com.nethcare.model.Patient;
import com.nethcare.model.Prescription;
import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.repository.BillRepository;
import com.nethcare.repository.ExaminationRepository;
import com.nethcare.repository.FrameRepository;
import com.nethcare.repository.LensRepository;
import com.nethcare.repository.OrderRepository;
import com.nethcare.repository.OrderStatusHistoryRepository;
import com.nethcare.repository.PatientRepository;
import com.nethcare.repository.PaymentRepository;
import com.nethcare.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orders;
    @Mock BillRepository bills;
    @Mock PaymentRepository payments;
    @Mock StockService stock;
    @Mock PatientRepository patients;
    @Mock PrescriptionRepository prescriptions;
    @Mock ExaminationRepository examinations;
    @Mock OrderStatusHistoryRepository statusHistory;
    @Mock FrameRepository frames;
    @Mock LensRepository lenses;

    OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, bills, payments, stock, patients,
                prescriptions, examinations, statusHistory, frames, lenses);
    }

    @Test
    void linksInStoreOrderToPatientAndCopiesPrescriptionSnapshot() {
        Patient patient = patient(7L);
        Prescription rx = prescription(12L, 7L);
        StockItem frame = stockItem(3L, StockCategory.FRAME);
        Order order = new Order();
        order.setPatientId(7L);
        order.setPrescriptionId(12L);
        OrderItem line = new OrderItem();
        line.setStockItemId(3L);
        line.setQuantity(1);

        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(prescriptions.findById(12L)).thenReturn(Optional.of(rx));
        when(stock.get(3L)).thenReturn(frame);
        when(orders.findByOrderNo("ORD-0001")).thenReturn(Optional.empty());
        when(orders.save(order)).thenAnswer(invocation -> {
            order.setId(20L);
            return order;
        });

        service.place(order, List.of(line), "staff");

        assertEquals("P-0007", order.getPatientNoSnapshot());
        assertEquals("Test Patient", order.getCustomerName());
        assertEquals("RX-0012", order.getPrescriptionNoSnapshot());
        assertEquals("-1.00", order.getOdSph());
        assertEquals(33L, order.getExaminationId());
        assertNotNull(order.getOrderedOn());
        verify(stock).reserve(3L, 1);
        verify(statusHistory).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsPrescriptionOwnedByAnotherPatient() {
        Patient patient = patient(7L);
        Prescription rx = prescription(12L, 8L);
        Order order = new Order();
        order.setPatientId(7L);
        order.setPrescriptionId(12L);

        when(patients.findById(7L)).thenReturn(Optional.of(patient));
        when(prescriptions.findById(12L)).thenReturn(Optional.of(rx));

        assertThrows(BusinessException.class,
                () -> service.place(order, List.of(new OrderItem()), "staff"));
    }

    private Patient patient(Long id) {
        Patient patient = new Patient();
        patient.setId(id);
        patient.setPatientNo("P-0007");
        patient.setFullName("Test Patient");
        patient.setPhone("0770000000");
        return patient;
    }

    private Prescription prescription(Long id, Long patientId) {
        Prescription rx = new Prescription();
        rx.setId(id);
        rx.setPatientId(patientId);
        rx.setExaminationId(33L);
        rx.setRxNo("RX-0012");
        rx.setIssuedOn(LocalDate.of(2026, 9, 20));
        rx.setOdSph("-1.00");
        return rx;
    }

    private StockItem stockItem(Long id, StockCategory category) {
        StockItem item = new StockItem();
        item.setId(id);
        item.setItemCode("FR-001");
        item.setName("Classic frame");
        item.setCategory(category);
        item.setUnitPrice(new BigDecimal("5000.00"));
        item.setQuantity(2);
        return item;
    }
}
