package com.tradeguard.surveillance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TradeSurveillanceListener {

    private static final Logger log = LoggerFactory.getLogger(TradeSurveillanceListener.class);

    private final SurveillanceService surveillance;

    public TradeSurveillanceListener(SurveillanceService surveillance) {
        this.surveillance = surveillance;
    }

    @TransactionalEventListener
    public void onTradeCommitted(TradeCommittedEvent event) {
        try {
            surveillance.processTrade(event.tradeId());
        } catch (RuntimeException ex) {
            log.error("Could not process surveillance for committed trade {}. "
                    + "Startup recovery will retry.", event.tradeId(), ex);
        }
    }
}
