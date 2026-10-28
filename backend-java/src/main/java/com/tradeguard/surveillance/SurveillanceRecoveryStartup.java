package com.tradeguard.surveillance;

import com.tradeguard.order.TradeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class SurveillanceRecoveryStartup implements ApplicationRunner {

    private final TradeRepository trades;
    private final SurveillanceService surveillance;

    public SurveillanceRecoveryStartup(TradeRepository trades,
                                       SurveillanceService surveillance) {
        this.trades = trades;
        this.surveillance = surveillance;
    }

    @Override
    public void run(ApplicationArguments args) {
        trades.findAllByOrderByIdAsc().forEach(trade ->
                surveillance.processTrade(trade.getId()));
    }
}
