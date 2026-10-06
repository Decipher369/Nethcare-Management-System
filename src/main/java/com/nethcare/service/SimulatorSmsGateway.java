package com.nethcare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "nethcare.sms.provider", havingValue = "simulator", matchIfMissing = true)
public class SimulatorSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatorSmsGateway.class);

    public SimulatorSmsGateway() {
    }

    @Override
    public String send(String destination, String message) {
        String receiptId = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        log.info("""
                
                ============================= [SMS SIMULATOR DISPATCH] =============================
                Destination:  {}
                Message Body: {}
                Receipt ID:   {}
                ====================================================================================
                """, destination, message, receiptId);

        return receiptId;
    }

    @Override
    public String getProviderName() {
        return "Simulator Mode (Virtual SMS Console)";
    }

    @Override
    public boolean isSimulator() {
        return true;
    }
}
