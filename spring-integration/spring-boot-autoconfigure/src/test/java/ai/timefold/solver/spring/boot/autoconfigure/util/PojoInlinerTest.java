package ai.timefold.solver.spring.boot.autoconfigure.util;

import static ai.timefold.solver.spring.boot.autoconfigure.util.PojoInliner.COMPLEX_POJO_MAP_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.config.solver.EnvironmentMode;

import org.junit.jupiter.api.Test;
import org.springframework.javapoet.CodeBlock;

public class PojoInlinerTest {
    private static class PrivatePojo {
        @Override
        public String toString() {
            return "PrivatePojo()";
        }
    }

    private record PrivateRecord() {
    }

    private enum PrivateEnum {
        VALUE
    }

    public static class BasicPojo {
        BasicPojo parentPojo;
        int id;
        String name;

        public BasicPojo() {
        }

        public BasicPojo(BasicPojo parentPojo, int id, String name) {
            this.parentPojo = parentPojo;
            this.id = id;
            this.name = name;
        }

        public BasicPojo getParentPojo() {
            return parentPojo;
        }

        public void setParentPojo(BasicPojo parentPojo) {
            this.parentPojo = parentPojo;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public record RecordPojo(String operation, int recordId, BasicPojo pojo) {
    }

    public static class LinkedRecordPojoReference {
        LinkedRecordPojo reference;

        public LinkedRecordPojo getReference() {
            return reference;
        }

        public void setReference(LinkedRecordPojo reference) {
            this.reference = reference;
        }

        @Override
        public String toString() {
            return "LinkedRecordPojoReference()";
        }
    }

    public record LinkedRecordPojo(LinkedRecordPojoReference next) {
        public static LinkedRecordPojo circular() {
            LinkedRecordPojoReference nextField = new LinkedRecordPojoReference();
            LinkedRecordPojo out = new LinkedRecordPojo(nextField);
            nextField.setReference(out);
            return out;
        }

        public static LinkedRecordPojo nonCircular() {
            LinkedRecordPojoReference nextField = new LinkedRecordPojoReference();
            nextField.setReference(new LinkedRecordPojo(null));
            return new LinkedRecordPojo(nextField);
        }
    }

    public record ArrayListRecord(ArrayList<?> arrayList) {
    }

    public static class ArrayListPojo {
        ArrayList<?> arrayList;

        public ArrayList<?> getArrayList() {
            return arrayList;
        }

        public void setArrayList(ArrayList<?> arrayList) {
            this.arrayList = arrayList;
        }

        @Override
        public String toString() {
            return "ArrayListPojo{" +
                    "arrayList=" + arrayList +
                    '}';
        }
    }

    public static class NotPojo {
        int aFieldWithSetter;
        int bFieldWithoutSetter;

        public NotPojo() {

        }

        public NotPojo(int aFieldWithSetter, int bFieldWithoutSetter) {
            this.aFieldWithSetter = aFieldWithSetter;
            this.bFieldWithoutSetter = bFieldWithoutSetter;
        }

        public int getAFieldWithSetter() {
            return aFieldWithSetter;
        }

        public void setAFieldWithSetter(int aFieldWithSetter) {
            this.aFieldWithSetter = aFieldWithSetter;
        }

        public int getBFieldWithoutSetter() {
            return bFieldWithoutSetter;
        }
    }

    public static class PrivateSetterPojo {
        int aFieldWithSetter;
        int bFieldWithSetter;

        public PrivateSetterPojo() {

        }

        public PrivateSetterPojo(int aFieldWithSetter, int bFieldWithSetter) {
            this.aFieldWithSetter = aFieldWithSetter;
            this.bFieldWithSetter = bFieldWithSetter;
        }

        public int getAFieldWithSetter() {
            return aFieldWithSetter;
        }

        public void setAFieldWithSetter(int aFieldWithSetter) {
            this.aFieldWithSetter = aFieldWithSetter;
        }

        public int getBFieldWithoutSetter() {
            return bFieldWithSetter;
        }

        private void setBFieldWithSetter(int bFieldWithSetter) {
            this.bFieldWithSetter = bFieldWithSetter;
        }
    }

    public static class ExtendedPojo extends BasicPojo {
        private String additionalField;

        public String getAdditionalField() {
            return additionalField;
        }

        public void setAdditionalField(String additionalField) {
            this.additionalField = additionalField;
        }
    }

    public static class ExtendedNotPojo extends PrivateSetterPojo {
        private String additionalField;

        public String getAdditionalField() {
            return additionalField;
        }

        public void setAdditionalField(String additionalField) {
            this.additionalField = additionalField;
        }
    }

    void assertBuilder(CodeBlock.Builder builder, String expected) {
        assertThat(builder.build().toString().trim()).isEqualTo(expected.trim());
    }

    private String getPojo(int id, Object value) {
        return CodeBlock.builder().add("(($T) $L.get($S))",
                value.getClass(),
                COMPLEX_POJO_MAP_FIELD_NAME,
                "$obj" + id).build().toString();
    }

    void assertAccessor(String accessor, int id, Object value) {
        assertThat(accessor).isEqualTo(getPojo(id, value));
    }

    @Test
    void inlinePrivateClasses() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        assertThatCode(() -> inliner.getInlinedPojo(PrivatePojo.class, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize (" + PrivatePojo.class + ") because it is not a public class.");

        assertThatCode(() -> inliner.getInlinedPojo(new PrivatePojo(), builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize (" + new PrivatePojo() + ") because its type (" + PrivatePojo.class
                        + ") is not public.");

        assertThatCode(() -> inliner.getInlinedPojo(new PrivateRecord(), builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize record (" + new PrivateRecord() + ") because its type (" + PrivateRecord.class
                        + ") is not public.");

        assertThatCode(() -> inliner.getInlinedPojo(PrivateEnum.VALUE, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize (" + PrivateEnum.VALUE + ") because its type (" + PrivateEnum.class
                        + ") is not a public class.");
    }

    @Test
    void inlinePrivateSetter() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        assertThatCode(() -> inliner.getInlinedPojo(new PrivateSetterPojo(1, 2), builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + PrivateSetterPojo.class
                        + ") as it is missing a public setter method for field (bFieldWithSetter) of type (int).");
    }

    @Test
    void inlineTypeUsingInterfaceImpl() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        ArrayListPojo pojo = new ArrayListPojo();
        pojo.setArrayList(new ArrayList<>());
        assertThatCode(() -> inliner.getInlinedPojo(pojo, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + ArrayListPojo.class
                        + ") as its field (arrayList) uses an implementation of a collection (" + ArrayList.class
                        + ") instead of the interface type (" + List.class + ").");

        ArrayListRecord record = new ArrayListRecord(new ArrayList<>());
        assertThatCode(() -> inliner.getInlinedPojo(record, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + ArrayListRecord.class
                        + ") as its component (arrayList) uses an implementation of a collection (" + ArrayList.class
                        + ") instead of the interface type (" + List.class + ").");
    }

    @Test
    void inlineNotPojo() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        NotPojo pojo = new NotPojo(1, 2);
        assertThatCode(() -> inliner.getInlinedPojo(pojo, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + NotPojo.class
                        + ") as it is missing a public setter method for field (bFieldWithoutSetter) of type (int).");
    }

    @Test
    void inlineExtendedNotPojo() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        assertThatCode(() -> inliner.getInlinedPojo(new ExtendedNotPojo(), builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + ExtendedNotPojo.class + ") because its superclass ("
                        + PrivateSetterPojo.class + ") is not serializable.")
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize type (" + PrivateSetterPojo.class
                        + ") as it is missing a public setter method for field (bFieldWithSetter) of type (int).");
    }

    @Test
    void inlineCircularRecord() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        LinkedRecordPojo pojo = LinkedRecordPojo.circular();

        assertThatCode(() -> inliner.getInlinedPojo(pojo, builder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize record (" + pojo + ") because the value (" + pojo.next
                        + ") for its component (next) is not serializable.")
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize object (" + pojo.next + ") because the value (" + pojo
                        + ") for its field (reference) is not serializable.")
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot serialize record (" + pojo
                        + ") because it is a record containing contains a circular reference.");
    }

    @Test
    void inlinePrimitives() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        // null
        assertThat(inliner.getInlinedPojo(null, builder)).isEqualTo("null");
        assertBuilder(builder, "");

        // numbers
        assertThat(inliner.getInlinedPojo(true, builder)).isEqualTo("true");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo(false, builder)).isEqualTo("false");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo((byte) 1, builder)).isEqualTo("((byte) 1)");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo((short) 1, builder)).isEqualTo("((short) 1)");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo(1, builder)).isEqualTo("1");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo(1L, builder)).isEqualTo("1L");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo(1f, builder)).isEqualTo("1.0f");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo(1d, builder)).isEqualTo("1.0d");
        assertBuilder(builder, "");

        // Strings and chars
        assertThat(inliner.getInlinedPojo('a', builder)).isEqualTo("'\\u0061'");
        assertBuilder(builder, "");
        assertThat(inliner.getInlinedPojo("my\nmultiline\nstring", builder)).isEqualTo("\"my\\nmultiline\\nstring\"");
        assertBuilder(builder, "");
    }

    @Test
    void inlineObjectPrimitives() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();

        // Classes
        assertThat(inliner.getInlinedPojo(PojoInliner.class, builder))
                .isEqualTo(PojoInliner.class.getCanonicalName() + ".class");
        assertBuilder(builder, "");

        // Enums
        assertThat(inliner.getInlinedPojo(EnvironmentMode.REPRODUCIBLE, builder))
                .isEqualTo(EnvironmentMode.class.getCanonicalName() + "." + EnvironmentMode.REPRODUCIBLE.name());
        assertBuilder(builder, "");

        // ClassLoader
        assertThat(inliner.getInlinedPojo(Thread.currentThread().getContextClassLoader(), builder))
                .isEqualTo("Thread.currentThread().getContextClassLoader()");
        assertBuilder(builder, "");
    }

    @Test
    void inlinePrimitiveArray() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        int[] pojo = new int[] { 1, 2, 3, 4, 5 };
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder,
                """
                        int[] $obj0;
                        $obj0 = new int[5];
                        $pojoMap.put("$obj0", $obj0);
                        $obj0[0] = 1;
                        $obj0[1] = 2;
                        $obj0[2] = 3;
                        $obj0[3] = 4;
                        $obj0[4] = 5;
                        """);
        assertAccessor(accessor, 0, pojo);
    }

    @Test
    void inlineObjectArray() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        Object[] pojo = new Object[3];
        pojo[0] = null;
        pojo[1] = pojo;
        pojo[2] = "Item";
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder,
                """
                        java.lang.Object[] $obj0;
                        $obj0 = new java.lang.Object[3];
                        $pojoMap.put("$obj0", $obj0);
                        $obj0[0] = null;
                        $obj0[1] = %s;
                        $obj0[2] = \"Item\";
                        """.formatted(getPojo(0, pojo)));
        assertAccessor(accessor, 0, pojo);
    }

    @Test
    void inlineIntList() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        List<Integer> pojo = List.of(1, 2, 3);
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder,
                """
                        java.util.List $obj0;
                        $obj0 = new java.util.ArrayList(3);
                        $pojoMap.put("$obj0", $obj0);
                        $obj0.add(1);
                        $obj0.add(2);
                        $obj0.add(3);
                        """);
        assertAccessor(accessor, 0, pojo);
    }

    @Test
    void inlineIntSet() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        Set<Integer> pojo = new LinkedHashSet<>(3);
        pojo.add(1);
        pojo.add(2);
        pojo.add(3);
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder,
                """
                        java.util.Set $obj0;
                        $obj0 = new java.util.LinkedHashSet(3);
                        $pojoMap.put("$obj0", $obj0);
                        $obj0.add(1);
                        $obj0.add(2);
                        $obj0.add(3);
                        """);
        assertAccessor(accessor, 0, pojo);
    }

    @Test
    void inlineIntStringMap() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        Map<Integer, String> pojo = new LinkedHashMap<>(3);
        pojo.put(1, "a");
        pojo.put(2, "b");
        pojo.put(3, "c");
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder,
                """
                        java.util.Map $obj0;
                        $obj0 = new java.util.LinkedHashMap(3);
                        $pojoMap.put("$obj0", $obj0);
                        $obj0.put(1, \"a\");
                        $obj0.put(2, \"b\");
                        $obj0.put(3, \"c\");
                        """);
        assertAccessor(accessor, 0, pojo);
    }

    @Test
    void inlinePojo() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        BasicPojo pojo = new BasicPojo(new BasicPojo(null, 0, "parent"), 1, "child");
        String accessor = inliner.getInlinedPojo(pojo, builder);
        String expected = """
                %s $obj0;
                $obj0 = new %s();
                $pojoMap.put("$obj0", $obj0);
                $obj0.setId(1);
                $obj0.setName("child");
                %s $obj1;
                $obj1 = new %s();
                $pojoMap.put("$obj1", $obj1);
                $obj1.setId(0);
                $obj1.setName("parent");
                $obj1.setParentPojo(null);
                $obj0.setParentPojo(%s);
                """.formatted(BasicPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                getPojo(1, pojo.getParentPojo()));
        assertBuilder(builder, expected);
        assertAccessor(accessor, 0, pojo);
        String parentAccessor = inliner.getInlinedPojo(pojo.getParentPojo(), builder);
        assertBuilder(builder, expected);
        assertAccessor(parentAccessor, 1, pojo.getParentPojo());
    }

    @Test
    void inlineExtendedPojo() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        ExtendedPojo pojo = new ExtendedPojo();
        pojo.setAdditionalField("newField");
        pojo.setId(1);
        pojo.setName("child");
        pojo.setParentPojo(new BasicPojo(null, 0, "parent"));
        String accessor = inliner.getInlinedPojo(pojo, builder);
        String expected = """
                %s $obj0;
                $obj0 = new %s();
                $pojoMap.put("$obj0", $obj0);
                $obj0.setAdditionalField("newField");
                $obj0.setId(1);
                $obj0.setName("child");
                %s $obj1;
                $obj1 = new %s();
                $pojoMap.put("$obj1", $obj1);
                $obj1.setId(0);
                $obj1.setName("parent");
                $obj1.setParentPojo(null);
                $obj0.setParentPojo(%s);
                """.formatted(ExtendedPojo.class.getCanonicalName(),
                ExtendedPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                getPojo(1, pojo.getParentPojo()));
        assertBuilder(builder, expected);
        assertAccessor(accessor, 0, pojo);
        String parentAccessor = inliner.getInlinedPojo(pojo.getParentPojo(), builder);
        assertBuilder(builder, expected);
        assertAccessor(parentAccessor, 1, pojo.getParentPojo());
    }

    @Test
    void inlineRecord() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        BasicPojo pojo = new BasicPojo(null, 0, "name");
        RecordPojo recordPojo = new RecordPojo("INSERT", 0, pojo);
        String accessor = inliner.getInlinedPojo(recordPojo, builder);
        String expected = """
                %s $obj0;
                $obj0 = new %s();
                $pojoMap.put("$obj0", $obj0);
                $obj0.setId(0);
                $obj0.setName("name");
                $obj0.setParentPojo(null);
                %s $obj1 = new %s("INSERT", 0, %s);
                $pojoMap.put("$obj1", $obj1);
                """.formatted(BasicPojo.class.getCanonicalName(),
                BasicPojo.class.getCanonicalName(),
                RecordPojo.class.getCanonicalName(),
                RecordPojo.class.getCanonicalName(),
                getPojo(0, recordPojo.pojo()));
        assertBuilder(builder, expected);
        assertAccessor(accessor, 1, recordPojo);
        String partAccessor = inliner.getInlinedPojo(recordPojo.pojo(), builder);
        assertBuilder(builder, expected);
        assertAccessor(partAccessor, 0, recordPojo.pojo());
    }

    @Test
    void inlineNonCircularRecord() {
        PojoInliner inliner = new PojoInliner();
        CodeBlock.Builder builder = CodeBlock.builder();
        LinkedRecordPojo pojo = LinkedRecordPojo.nonCircular();
        String expected = """
                %s $obj0;
                $obj0 = new %s();
                $pojoMap.put("$obj0", $obj0);
                %s $obj1 = new %s(null);
                $pojoMap.put("$obj1", $obj1);
                $obj0.setReference(%s);
                %s $obj2 = new %s(%s);
                $pojoMap.put("$obj2", $obj2);
                """.formatted(LinkedRecordPojoReference.class.getCanonicalName(),
                LinkedRecordPojoReference.class.getCanonicalName(),
                LinkedRecordPojo.class.getCanonicalName(),
                LinkedRecordPojo.class.getCanonicalName(),
                getPojo(1, pojo.next().getReference()),
                LinkedRecordPojo.class.getCanonicalName(),
                LinkedRecordPojo.class.getCanonicalName(),
                getPojo(0, pojo.next()));
        String accessor = inliner.getInlinedPojo(pojo, builder);
        assertBuilder(builder, expected);
        assertAccessor(accessor, 2, pojo);
        String nextAccessor = inliner.getInlinedPojo(pojo.next(), builder);
        assertBuilder(builder, expected);
        assertAccessor(nextAccessor, 0, pojo.next());
        String referenceAccessor = inliner.getInlinedPojo(pojo.next().getReference(), builder);
        assertBuilder(builder, expected);
        assertAccessor(referenceAccessor, 1, pojo.next().getReference());
    }
}
