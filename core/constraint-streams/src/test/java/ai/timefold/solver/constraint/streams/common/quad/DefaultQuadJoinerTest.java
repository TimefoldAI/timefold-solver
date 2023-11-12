package ai.timefold.solver.constraint.streams.common.quad;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DefaultQuadJoinerTest {
 @Test
 void equals_shouldReturnFalse_whenGivenNull() {
 // Arrange
 DefaultQuadJoiner<?, ?, ?, ?> joiner = new 
DefaultQuadJoiner<>(null, null, null);
 // Act
 boolean result = joiner.equals(null);
 // Assert
 assertFalse(result);
 }
