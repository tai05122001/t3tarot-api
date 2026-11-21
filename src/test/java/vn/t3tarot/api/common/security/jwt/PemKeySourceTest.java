package vn.t3tarot.api.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import vn.t3tarot.api.common.security.TestJwtFactory;

class PemKeySourceTest {

    private static final String KEY_NAME = "JWT public key";

    @TempDir
    Path tempDir;

    @Test
    void returnsInlineKeyWhenOnlyInlineIsSet() {
        String inlineKey = TestJwtFactory.trustedPublicKeyBase64();

        assertThat(PemKeySource.read(inlineKey, null, KEY_NAME)).isEqualTo(inlineKey);
    }

    @Test
    void readsPemFileWhenOnlyLocationIsSet() throws IOException {
        Path pemFile = Files.writeString(tempDir.resolve("jwt-public.pem"), TestJwtFactory.trustedPublicKeyPem());

        String pem = PemKeySource.read(null, new FileSystemResource(pemFile), KEY_NAME);

        assertThat(RsaPublicKeys.parse(pem)).isEqualTo(RsaPublicKeys.parse(TestJwtFactory.trustedPublicKeyBase64()));
    }

    @Test
    void treatsBlankInlineKeyAsUnsetWhenLocationIsSet() throws IOException {
        Path pemFile = Files.writeString(tempDir.resolve("jwt-public.pem"), TestJwtFactory.trustedPublicKeyPem());

        assertThat(PemKeySource.read("  ", new FileSystemResource(pemFile), KEY_NAME))
                .isEqualTo(TestJwtFactory.trustedPublicKeyPem());
    }

    @Test
    void rejectsBothInlineKeyAndLocation() throws IOException {
        Path pemFile = Files.writeString(tempDir.resolve("jwt-public.pem"), TestJwtFactory.trustedPublicKeyPem());
        FileSystemResource location = new FileSystemResource(pemFile);
        String inlineKey = TestJwtFactory.trustedPublicKeyBase64();

        assertThatThrownBy(() -> PemKeySource.read(inlineKey, location, KEY_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("both inline and by location")
                .hasMessageNotContaining(inlineKey);
    }

    @Test
    void rejectsMissingInlineKeyAndLocation() {
        assertThatThrownBy(() -> PemKeySource.read("", null, KEY_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT public key is missing");
    }

    @Test
    void rejectsUnreadableLocation() {
        FileSystemResource missingFile = new FileSystemResource(tempDir.resolve("absent.pem"));

        assertThatThrownBy(() -> PemKeySource.read(null, missingFile, KEY_NAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("file is not readable")
                .hasCauseInstanceOf(IOException.class);
    }
}
