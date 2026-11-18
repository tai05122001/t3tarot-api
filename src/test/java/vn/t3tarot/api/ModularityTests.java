package vn.t3tarot.api;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void moduleBoundariesAreRespected() {
        ApplicationModules modules = ApplicationModules.of(T3TarotApiApplication.class);

        assertThatCode(modules::verify).doesNotThrowAnyException();
    }
}
