package ai.timefold.solver.core.config.heuristic.selector.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import 
ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelec
tionConfig;
import 
ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConf
ig;
import 
ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig
;
import org.junit.jupiter.api.Test;
public class DestinationSelectorConfigTest {
 @Test
 public void testVisitReferencedClasses() {
 // Setup
 EntitySelectorConfig entitySelectorConfig = 
mock(EntitySelectorConfig.class);
 ValueSelectorConfig valueSelectorConfig = 
mock(ValueSelectorConfig.class);
 NearbySelectionConfig nearbySelectionConfig = 
mock(NearbySelectionConfig.class);
 DestinationSelectorConfig destinationSelectorConfig = new 
DestinationSelectorConfig(entitySelectorConfig,
 valueSelectorConfig, nearbySelectionConfig);
 Set<Class<?>> visitedClasses = new HashSet<>();
 // Test
 Consumer<Class<?>> classVisitor = visitedClasses::add;
 destinationSelectorConfig.visitReferencedClasses(classVisitor);
 // Verify
 assertEquals(0, visitedClasses.size());
 assertEquals(false, 
visitedClasses.contains(EntitySelectorConfig.class));
 assertEquals(false, 
visitedClasses.contains(ValueSelectorConfig.class));
 assertEquals(false, 
visitedClasses.contains(NearbySelectionConfig.class));
 }
}
