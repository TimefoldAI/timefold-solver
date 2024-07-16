package ai.timefold.jpyinterpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class BytecodeSwitchImplementor {

    public static void createStringSwitch(MethodVisitor methodVisitor, Collection<String> keyNames,
            int switchVariable, Consumer<String> caseWriter, Runnable defaultCase, boolean doesEachCaseReturnEarly) {
        if (keyNames.isEmpty()) {
            defaultCase.run();
            return;
        }

        // keys in lookup switch MUST be sorted
        SortedMap<Integer, List<String>> hashCodeToMatchingFieldList = new TreeMap<>();
        for (String fieldName : keyNames) {
            hashCodeToMatchingFieldList.computeIfAbsent(fieldName.hashCode(), hash -> new ArrayList<>()).add(fieldName);
        }

        int[] keys = new int[hashCodeToMatchingFieldList.size()];
        Label[] hashCodeLabels = new Label[keys.length];

        { // Scoped to hide keyIndex
            int keyIndex = 0;
            for (Integer key : hashCodeToMatchingFieldList.keySet()) {
                keys[keyIndex] = key;
                hashCodeLabels[keyIndex] = new Label();
                keyIndex++;
            }
        }

        final int TOS_VARIABLE = switchVariable;
        final int CASE_VARIABLE = TOS_VARIABLE + 1;

        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitVarInsn(Opcodes.ASTORE, TOS_VARIABLE);
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(String.class), "hashCode",
                Type.getMethodDescriptor(Type.INT_TYPE), false);
        Map<Integer, String> switchVariableValueToField = new HashMap<>();

        Label endOfKeySwitch = new Label();

        methodVisitor.visitLdcInsn(-1);
        methodVisitor.visitVarInsn(Opcodes.ISTORE, CASE_VARIABLE);

        methodVisitor.visitLookupSwitchInsn(endOfKeySwitch, keys, hashCodeLabels);

        int totalEntries = 0;
        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitLabel(hashCodeLabels[i]);
            List<String> matchingFields = hashCodeToMatchingFieldList.get(keys[i]);

            for (int fieldIndex = 0; fieldIndex < matchingFields.size(); fieldIndex++) {
                String field = matchingFields.get(fieldIndex);
                Label ifDoesNotMatchLabel = new Label();

                methodVisitor.visitVarInsn(Opcodes.ALOAD, TOS_VARIABLE);
                methodVisitor.visitLdcInsn(field);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(String.class), "equals",
                        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class)), false);
                if (fieldIndex != matchingFields.size() - 1) {
                    methodVisitor.visitJumpInsn(Opcodes.IFEQ, ifDoesNotMatchLabel);
                } else {
                    methodVisitor.visitJumpInsn(Opcodes.IFEQ, endOfKeySwitch);
                }
                methodVisitor.visitLdcInsn(totalEntries);
                methodVisitor.visitVarInsn(Opcodes.ISTORE, CASE_VARIABLE);
                if (fieldIndex != matchingFields.size() - 1) {
                    methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfKeySwitch);
                    methodVisitor.visitLabel(ifDoesNotMatchLabel);
                }

                switchVariableValueToField.put(totalEntries, field);
                totalEntries++;
            }
            if (totalEntries != keyNames.size()) {
                methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfKeySwitch);
            }
        }
        methodVisitor.visitLabel(endOfKeySwitch);

        Label missingField = new Label();
        Label endOfFieldsSwitch = new Label();
        Label[] fieldHandlerLabels = new Label[totalEntries];
        for (int i = 0; i < fieldHandlerLabels.length; i++) {
            fieldHandlerLabels[i] = new Label();
        }

        methodVisitor.visitVarInsn(Opcodes.ILOAD, CASE_VARIABLE);
        methodVisitor.visitTableSwitchInsn(0, totalEntries - 1, missingField, fieldHandlerLabels);

        for (int i = 0; i < totalEntries; i++) {
            methodVisitor.visitLabel(fieldHandlerLabels[i]);
            String field = switchVariableValueToField.get(i);
            caseWriter.accept(field);
            if (!doesEachCaseReturnEarly) {
                methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfFieldsSwitch);
            }
        }
        methodVisitor.visitLabel(missingField);
        defaultCase.run();
        if (!doesEachCaseReturnEarly) {
            methodVisitor.visitLabel(endOfFieldsSwitch);
        }
    }

    public static void createIntSwitch(MethodVisitor methodVisitor, Collection<Integer> keySet,
            Consumer<Integer> caseWriter, Runnable defaultCase,
            boolean doesEachCaseReturnEarly) {
        if (keySet.isEmpty()) {
            defaultCase.run();
            return;
        }

        List<Integer> sortedKeyList = keySet.stream().sorted().collect(Collectors.toList());

        int[] keys = new int[sortedKeyList.size()];
        Label[] keyLabels = new Label[keys.length];

        { // Scoped to hide keyIndex
            int keyIndex = 0;
            for (Integer key : sortedKeyList) {
                keys[keyIndex] = key;
                keyLabels[keyIndex] = new Label();
                keyIndex++;
            }
        }

        Label endOfKeySwitch = new Label();
        Label missingKey = new Label();

        methodVisitor.visitLookupSwitchInsn(missingKey, keys, keyLabels);

        for (int i = 0; i < keys.length; i++) {
            methodVisitor.visitLabel(keyLabels[i]);
            Integer matchingKey = sortedKeyList.get(i);

            caseWriter.accept(matchingKey);
            if (!doesEachCaseReturnEarly) {
                methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfKeySwitch);
            }
        }
        methodVisitor.visitLabel(missingKey);

        defaultCase.run();

        if (!doesEachCaseReturnEarly) {
            methodVisitor.visitLabel(endOfKeySwitch);
        }
    }
}
