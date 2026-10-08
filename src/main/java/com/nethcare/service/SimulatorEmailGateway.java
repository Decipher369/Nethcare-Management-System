package com.nethcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "nethcare.email.provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatorEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatorEmailGateway.class);

    public SimulatorEmailGateway() {
    }

    @Override
    public String send(String destination, String subject, String message) {
        String receiptId = "SIM-MAIL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        log.info("""
                
                ============================ [EMAIL SIMULATOR DISPATCH] ============================
                Destination:  {}
                Subject:      {}
                Message Body: {}
                Receipt ID:   {}
                ====================================================================================
                """, destination, subject, message, receiptId);

        return receiptId;
    }

    @Override
    public String getProviderName() {
        return "Simulator Mode (Virtual Email Console)";
    }

    @Override
    public boolean isSimulator() {
        return true;
    }
}
