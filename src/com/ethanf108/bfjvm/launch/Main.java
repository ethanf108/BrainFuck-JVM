package com.ethanf108.bfjvm.launch;

import edu.rit.csh.intraspect.data.ClassFile;
import edu.rit.csh.intraspect.data.MajorVersion;
import edu.rit.csh.intraspect.data.MethodDesc;
import edu.rit.csh.intraspect.data.attribute.AttributeDesc;
import edu.rit.csh.intraspect.data.attribute.CodeAttribute;
import edu.rit.csh.intraspect.data.attribute.StackMapTableAttribute;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.FullFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.SameFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.StackMapFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.IntegerVariableInfo;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.ObjectVariableInfo;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.VerificationTypeInfo;
import edu.rit.csh.intraspect.data.constant.ClassConstant;
import edu.rit.csh.intraspect.data.constant.FieldRefConstant;
import edu.rit.csh.intraspect.data.constant.MethodRefConstant;
import edu.rit.csh.intraspect.data.constant.NameAndTypeConstant;
import edu.rit.csh.intraspect.data.instruction.Instruction;
import edu.rit.csh.intraspect.data.instruction.branch.IfneInstruction;
import edu.rit.csh.intraspect.data.instruction.constant.BipushInstruction;
import edu.rit.csh.intraspect.data.instruction.constant.IConst_0Instruction;
import edu.rit.csh.intraspect.data.instruction.constant.SipushInstruction;
import edu.rit.csh.intraspect.data.instruction.control.ReturnInstruction;
import edu.rit.csh.intraspect.data.instruction.field.GetStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.invoke.InvokeVirtualInstruction;
import edu.rit.csh.intraspect.data.instruction.load.ALoad_1Instruction;
import edu.rit.csh.intraspect.data.instruction.load.BALoadInstruction;
import edu.rit.csh.intraspect.data.instruction.load.ILoad_0Instruction;
import edu.rit.csh.intraspect.data.instruction.math.add.IAddInstruction;
import edu.rit.csh.intraspect.data.instruction.misc.IincInstruction;
import edu.rit.csh.intraspect.data.instruction.object.NewArrayInstruction;
import edu.rit.csh.intraspect.data.instruction.stack.Dup2Instruction;
import edu.rit.csh.intraspect.data.instruction.store.AStore_1Instruction;
import edu.rit.csh.intraspect.data.instruction.store.BAStoreInstruction;
import edu.rit.csh.intraspect.data.instruction.store.IStore_0Instruction;
import edu.rit.csh.intraspect.util.OffsetOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Main {

    private static int getIndex(List<Instruction> list) {
        OffsetOutputStream out = new OffsetOutputStream(OutputStream.nullOutputStream());
        list.forEach(n -> {
            try {
                n.write(out);
            } catch (IOException e) {
            }
        });
        return (int) out.getCounter();
    }

    public static void main(String[] args) throws IOException {
        final String CODE = "++++[>++++[>++++<-]<-]>>+.";
        final String fileName = "bf";
        ClassFile cf = new ClassFile();
        cf.setThisClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent(fileName))));
        cf.setSuperClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/Object"))));
        cf.setFlag(ClassFile.AccessFlag.PUBLIC);
        cf.setMajorVersion(new MajorVersion(61));
        cf.setMinorVersion(0);
        final int ca = cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("[B")));

        final int systemClass = cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/System")));

        final int soutMethod = cf.addConstant(new MethodRefConstant(
                cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/io/PrintStream"))),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("print"),
                        cf.putUTFIfAbsent("(C)V")
                ))
        ));
        final int soutField = cf.addConstant(new FieldRefConstant(
                systemClass,
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("out"),
                        cf.putUTFIfAbsent("Ljava/io/PrintStream;")
                ))
        ));

        final int sinMethod = cf.addConstant(new MethodRefConstant(
                cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/io/InputStream"))),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("read"),
                        cf.putUTFIfAbsent("()I")
                ))
        ));

        final int sinField = cf.addConstant(new FieldRefConstant(
                systemClass,
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("in"),
                        cf.putUTFIfAbsent("Ljava/io/InputStream;")
                ))
        ));

        List<Instruction> inst = new ArrayList<>();
        List<StackMapFrame> smf = new ArrayList<>();


        inst.addAll(List.of(
                new IConst_0Instruction(),
                new IStore_0Instruction(),
                new SipushInstruction((short) 10000),
                new NewArrayInstruction(8),
                new AStore_1Instruction()
        ));

        smf.add(new FullFrame(
                getIndex(inst),
                new VerificationTypeInfo[]{
                        new IntegerVariableInfo(),
                        new ObjectVariableInfo(7, ca)
                },
                new VerificationTypeInfo[0]
        ));
        Stack<Integer> jumps = new Stack<>();
        List<Integer> sm = new ArrayList<>();

        int total = 0;
        for (char c : CODE.toCharArray()) {
            if (c == '+') {
                total++;
            } else if (c == '-') {
                total--;
            } else if (total != 0) {
                inst.addAll(List.of(
                        new ALoad_1Instruction(),
                        new ILoad_0Instruction(),
                        new Dup2Instruction(),
                        new BALoadInstruction(),
                        new BipushInstruction((byte) total),
                        new IAddInstruction(),
                        new BAStoreInstruction()));
                total = 0;
            }
            if (c == '>') {
                inst.add(new IincInstruction(0, 1));
            } else if (c == '<') {
                inst.add(new IincInstruction(0, -1));
            } else if (c == '.') {
                inst.addAll(List.of(
                        new GetStaticInstruction(soutField),
                        new ALoad_1Instruction(),
                        new ILoad_0Instruction(),
                        new BALoadInstruction(),
                        new InvokeVirtualInstruction(soutMethod)
                ));
            } else if (c == ',') {
                inst.addAll(List.of(
                        new ALoad_1Instruction(),
                        new ILoad_0Instruction(),
                        new GetStaticInstruction(sinField),
                        new InvokeVirtualInstruction(sinMethod),
                        new BAStoreInstruction()
                ));
            } else if (c == '[') {
                final int index = getIndex(inst);
                jumps.push(index);
                sm.add(index);
            } else if (c == ']') {
                inst.addAll(List.of(
                        new ALoad_1Instruction(),
                        new IConst_0Instruction(),
                        new BALoadInstruction()
                ));

                final int jumpDelta = jumps.pop() - getIndex(inst);
                inst.add(new IfneInstruction(jumpDelta));
            }
        }
        inst.add(new ReturnInstruction());
        int last = 8;
        for (int i : sm) {
            if (i - last - 1 > 63) {
                smf.add(new FullFrame(
                        i - last - 1,
                        new VerificationTypeInfo[]{
                                new IntegerVariableInfo(),
                                new ObjectVariableInfo(7, ca)
                        },
                        new VerificationTypeInfo[0]
                ));
            } else {
                smf.add(new SameFrame(i - last - 1));
            }
            last = i;
        }
        MethodDesc main = new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PUBLIC, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("main"),
                cf.putUTFIfAbsent("([Ljava/lang/String;)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                5,
                                2,
                                inst.toArray(Instruction[]::new),
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[]{
                                        new StackMapTableAttribute(
                                                cf.putUTFIfAbsent("StackMapTable"),
                                                smf.toArray(StackMapFrame[]::new)
                                        )
                                }
                        )
                }
        );
        cf.addMethod(main);
        FileOutputStream out = new FileOutputStream("/path/to/file");
        cf.write(out);
        out.close();
    }
}
