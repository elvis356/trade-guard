ALTER TABLE instruments
    ADD COLUMN reference_price DECIMAL(12, 4) NOT NULL DEFAULT 100.00;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_order_id BINARY(16) NOT NULL,
    seq_no BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    side VARCHAR(4) NOT NULL,
    limit_price DECIMAL(12, 4) NOT NULL,
    original_quantity BIGINT NOT NULL,
    remaining_quantity BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_orders_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_orders_status CHECK (
        status IN ('NEW', 'PARTIALLY_FILLED', 'FILLED', 'CANCELED')
    ),
    CONSTRAINT chk_orders_quantity CHECK (
        original_quantity > 0 AND remaining_quantity >= 0
        AND remaining_quantity <= original_quantity
    ),
    CONSTRAINT chk_orders_price CHECK (limit_price > 0),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_orders_instrument FOREIGN KEY (symbol) REFERENCES instruments(symbol),
    CONSTRAINT uk_orders_user_client_id UNIQUE (user_id, client_order_id),
    CONSTRAINT uk_orders_symbol_seq_no UNIQUE (symbol, seq_no)
);

CREATE INDEX idx_orders_book_buy
    ON orders (symbol, side, status, remaining_quantity, limit_price DESC, id ASC);
CREATE INDEX idx_orders_book_sell
    ON orders (symbol, side, status, remaining_quantity, limit_price ASC, id ASC);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC, id DESC);

CREATE TABLE trades (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(16) NOT NULL,
    buy_order_id BIGINT NOT NULL,
    sell_order_id BIGINT NOT NULL,
    price DECIMAL(12, 4) NOT NULL,
    quantity BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT chk_trades_price CHECK (price > 0),
    CONSTRAINT chk_trades_quantity CHECK (quantity > 0),
    CONSTRAINT chk_trades_distinct_orders CHECK (buy_order_id <> sell_order_id),
    CONSTRAINT fk_trades_instrument FOREIGN KEY (symbol) REFERENCES instruments(symbol),
    CONSTRAINT fk_trades_buy_order FOREIGN KEY (buy_order_id) REFERENCES orders(id),
    CONSTRAINT fk_trades_sell_order FOREIGN KEY (sell_order_id) REFERENCES orders(id)
);

CREATE INDEX idx_trades_symbol_created ON trades (symbol, created_at DESC, id DESC);
