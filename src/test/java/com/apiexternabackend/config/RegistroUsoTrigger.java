package com.apiexternabackend.config;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Trigger H2 só de teste: trava ou derruba o INSERT em registro_uso no próprio banco, sem trocar o
 * repositório por um spy chamado ao mesmo tempo pela thread do teste e pela da gravação.
 */
public class RegistroUsoTrigger implements Trigger {

    static volatile CountDownLatch travar;
    static volatile boolean falhar;

    @Override
    public void fire(Connection conexao, Object[] antiga, Object[] nova) throws SQLException {
        if (falhar) {
            throw new SQLException("banco fora do ar");
        }
        CountDownLatch trava = travar;
        if (trava != null) {
            try {
                trava.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
