package com.ethanf108.bfjvm.launch;

import edu.rit.csh.intraspect.data.ClassFile;
import edu.rit.csh.intraspect.data.FieldDesc;
import edu.rit.csh.intraspect.data.MajorVersion;
import edu.rit.csh.intraspect.data.MethodDesc;
import edu.rit.csh.intraspect.data.attribute.AttributeDesc;
import edu.rit.csh.intraspect.data.attribute.CodeAttribute;
import edu.rit.csh.intraspect.data.attribute.StackMapTableAttribute;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.FullFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.SameFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.StackMapFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.VerificationTypeInfo;
import edu.rit.csh.intraspect.data.constant.ClassConstant;
import edu.rit.csh.intraspect.data.constant.FieldRefConstant;
import edu.rit.csh.intraspect.data.constant.MethodRefConstant;
import edu.rit.csh.intraspect.data.constant.NameAndTypeConstant;
import edu.rit.csh.intraspect.data.instruction.Instruction;
import edu.rit.csh.intraspect.data.instruction.branch.GotoInstruction;
import edu.rit.csh.intraspect.data.instruction.branch.IfeqInstruction;
import edu.rit.csh.intraspect.data.instruction.constant.BipushInstruction;
import edu.rit.csh.intraspect.data.instruction.constant.IConst_0Instruction;
import edu.rit.csh.intraspect.data.instruction.constant.SipushInstruction;
import edu.rit.csh.intraspect.data.instruction.control.IReturnInstruction;
import edu.rit.csh.intraspect.data.instruction.control.ReturnInstruction;
import edu.rit.csh.intraspect.data.instruction.field.GetStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.field.PutStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.invoke.InvokeStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.invoke.InvokeVirtualInstruction;
import edu.rit.csh.intraspect.data.instruction.load.BALoadInstruction;
import edu.rit.csh.intraspect.data.instruction.load.ILoad_0Instruction;
import edu.rit.csh.intraspect.data.instruction.math.add.IAddInstruction;
import edu.rit.csh.intraspect.data.instruction.object.NewArrayInstruction;
import edu.rit.csh.intraspect.data.instruction.store.BAStoreInstruction;
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
                if (n == null) { //ifeq placeholder
                    new IfeqInstruction(0).write(out);
                } else {
                    n.write(out);
                }
            } catch (IOException e) {
            }
        });
        return (int) out.getCounter();
    }

    public static void main(String[] args) throws IOException {
        final String CODE = ">++++++++[<+++++++++>-]<.>++++[<+++++++>-]<+.+++++++..+++.>>++++++[<+++++++>-]<+\n" +
                "+.------------.>++++++[<+++++++++>-]<+.<.+++.------.--------.>>>++++[<++++++++>-\n" +
                "]<+.[-]++++++++++.";
        final String fileName = "bf";
        final int DATA_SIZE = 10000;
        ClassFile cf = new ClassFile();
        cf.setThisClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent(fileName))));
        cf.setSuperClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/Object"))));
        cf.setFlag(ClassFile.AccessFlag.PUBLIC);
        cf.setMajorVersion(new MajorVersion(61));
        cf.setMinorVersion(0);

        final int systemClass = cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/System")));

        cf.addField(new FieldDesc(
                FieldDesc.combineFlags(FieldDesc.AccessFlag.PRIVATE, FieldDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("pointer"),
                cf.putUTFIfAbsent("I"),
                new AttributeDesc[0]
        ));

        final int pointerField = cf.addConstant(new FieldRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("pointer"),
                        cf.putUTFIfAbsent("I")
                ))
        ));

        cf.addField(new FieldDesc(
                FieldDesc.combineFlags(FieldDesc.AccessFlag.PRIVATE, FieldDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("data"),
                cf.putUTFIfAbsent("[B"),
                new AttributeDesc[0]
        ));

        final int dataField = cf.addConstant(new FieldRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("data"),
                        cf.putUTFIfAbsent("[B")
                ))
        ));


        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("get"),
                cf.putUTFIfAbsent("()B"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                0,
                                new Instruction[]{
                                        new GetStaticInstruction(dataField),
                                        new GetStaticInstruction(pointerField),
                                        new BALoadInstruction(),
                                        new IReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));

        final int getMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("get"),
                        cf.putUTFIfAbsent("()B")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("put"),
                cf.putUTFIfAbsent("(B)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                3,
                                1,
                                new Instruction[]{
                                        new GetStaticInstruction(dataField),
                                        new GetStaticInstruction(pointerField),
                                        new ILoad_0Instruction(),
                                        new BAStoreInstruction(),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));

        final int setMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("put"),
                        cf.putUTFIfAbsent("(B)V")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("print"),
                cf.putUTFIfAbsent("()V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                0,
                                new Instruction[]{
                                        new GetStaticInstruction(cf.addConstant(new FieldRefConstant(
                                                systemClass,
                                                cf.addConstant(new NameAndTypeConstant(
                                                        cf.putUTFIfAbsent("out"),
                                                        cf.putUTFIfAbsent("Ljava/io/PrintStream;")
                                                ))
                                        ))),
                                        new InvokeStaticInstruction(getMethod),
                                        new InvokeVirtualInstruction(
                                                cf.addConstant(new MethodRefConstant(
                                                        cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/io/PrintStream"))),
                                                        cf.addConstant(new NameAndTypeConstant(
                                                                cf.putUTFIfAbsent("print"),
                                                                cf.putUTFIfAbsent("(C)V")
                                                        ))
                                                ))
                                        ),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));
        final int printMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("print"),
                        cf.putUTFIfAbsent("()V")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("read"),
                cf.putUTFIfAbsent("()V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                3,
                                0,
                                new Instruction[]{
                                        new GetStaticInstruction(dataField),
                                        new GetStaticInstruction(pointerField),
                                        new GetStaticInstruction(cf.addConstant(new FieldRefConstant(
                                                systemClass,
                                                cf.addConstant(new NameAndTypeConstant(
                                                        cf.putUTFIfAbsent("in"),
                                                        cf.putUTFIfAbsent("Ljava/io/InputStream;")
                                                ))
                                        ))),
                                        new InvokeVirtualInstruction(cf.addConstant(new MethodRefConstant(
                                                cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/io/InputStream"))),
                                                cf.addConstant(new NameAndTypeConstant(
                                                        cf.putUTFIfAbsent("read"),
                                                        cf.putUTFIfAbsent("()I")
                                                ))
                                        ))),
                                        new BAStoreInstruction(),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));

        final int readMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("read"),
                        cf.putUTFIfAbsent("()V")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("+\u0007-"),
                cf.putUTFIfAbsent("(B)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                1,
                                new Instruction[]{
                                        new InvokeStaticInstruction(getMethod),
                                        new ILoad_0Instruction(),
                                        new IAddInstruction(),
                                        new InvokeStaticInstruction(setMethod),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));

        final int plusMinusMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("+\u0007-"),
                        cf.putUTFIfAbsent("(B)V")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("shift"),
                cf.putUTFIfAbsent("(I)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                1,
                                new Instruction[]{
                                        new GetStaticInstruction(pointerField),
                                        new ILoad_0Instruction(),
                                        new IAddInstruction(),
                                        new PutStaticInstruction(pointerField),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[0]
                        )
                }
        ));

        final int shiftMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("shift"),
                        cf.putUTFIfAbsent("(I)V")
                ))
        ));

        List<Instruction> mainMethodCode = new ArrayList<>();
        Stack<Integer> openBracketPositions = new Stack<>();
        List<Integer> allJumpPositions = new ArrayList<>();

        mainMethodCode.addAll(List.of(
                new IConst_0Instruction(),
                new PutStaticInstruction(pointerField),
                new SipushInstruction((short) DATA_SIZE),
                new NewArrayInstruction(8),
                new PutStaticInstruction(dataField)
        ));

        int plusMinus = 0;
        int shift = 0;

        for (char c : CODE.toCharArray()) {
            if (c == '+') {
                plusMinus++;
            } else if (c == '-') {
                plusMinus--;
            } else if (plusMinus != 0) {
                mainMethodCode.addAll(List.of(
                        new BipushInstruction((byte) plusMinus),
                        new InvokeStaticInstruction(plusMinusMethod)
                ));
                plusMinus = 0;
            }
            if (c == '>') {
                shift++;
            } else if (c == '<') {
                shift--;
            } else if (shift != 0) {
                mainMethodCode.addAll(List.of(
                        new BipushInstruction((byte) shift),
                        new InvokeStaticInstruction(shiftMethod)
                ));
                shift = 0;
            }
            if (c == '.') {
                mainMethodCode.add(new InvokeStaticInstruction(printMethod));
            } else if (c == ',') {
                mainMethodCode.add(new InvokeStaticInstruction(readMethod));
            } else if (c == '[') {
                openBracketPositions.push(getIndex(mainMethodCode));
                allJumpPositions.add(getIndex(mainMethodCode));
                mainMethodCode.add(new InvokeStaticInstruction(getMethod));
                mainMethodCode.add(null);
            } else if (c == ']') {
                final int jumpPos = openBracketPositions.pop();
                mainMethodCode.add(new GotoInstruction(jumpPos - getIndex(mainMethodCode)));
                mainMethodCode.set(mainMethodCode.lastIndexOf(null), new IfeqInstruction(getIndex(mainMethodCode) - jumpPos - 3));
                allJumpPositions.add(getIndex(mainMethodCode));
            }
        }

        mainMethodCode.add(new ReturnInstruction());

        List<StackMapFrame> stackMapFrames = new ArrayList<>();

        int lastPosition = -1;
        for (int position : allJumpPositions) {
            if (lastPosition == -1 || position - lastPosition - 1 > 63) {
                stackMapFrames.add(new FullFrame(
                        position - lastPosition - 1,
                        new VerificationTypeInfo[0],
                        new VerificationTypeInfo[0]
                ));
            } else {
                stackMapFrames.add(new SameFrame(position - lastPosition - 1));
            }
            lastPosition = position;
        }

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PUBLIC, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("main"),
                cf.putUTFIfAbsent("([Ljava/lang/String;)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                1,
                                1,
                                mainMethodCode.toArray(Instruction[]::new),
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[]{
                                        new StackMapTableAttribute(
                                                cf.putUTFIfAbsent("StackMapTable"),
                                                stackMapFrames.toArray(StackMapFrame[]::new)
                                        )
                                }
                        )
                }
        ));

        cf.write(new FileOutputStream("/home/ethan/bf.class"));
    }
}
