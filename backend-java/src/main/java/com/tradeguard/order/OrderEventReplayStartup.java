package com.tradeguard.order;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OrderEventReplayStartup implements ApplicationRunner {

    private final OrderEventReplayer replayer;

    public OrderEventReplayStartup(OrderEventReplayer replayer) {
        this.replayer = replayer;
    }

    @Override
    public void run(ApplicationArguments args) {
        replayer.replay();
    }
}
