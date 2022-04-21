package com.ethanf108.bfjvm.launch;

import edu.rit.csh.intraspect.data.ClassFile;
import edu.rit.csh.intraspect.data.FieldDesc;
import edu.rit.csh.intraspect.data.MajorVersion;
import edu.rit.csh.intraspect.data.MethodDesc;
import edu.rit.csh.intraspect.data.attribute.AttributeDesc;
import edu.rit.csh.intraspect.data.attribute.CodeAttribute;
import edu.rit.csh.intraspect.data.attribute.SourceFileAttribute;
import edu.rit.csh.intraspect.data.attribute.StackMapTableAttribute;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.FullFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.SameFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.StackMapFrame;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.IntegerVariableInfo;
import edu.rit.csh.intraspect.data.attribute.stackmaptable.verificationtypeinfo.VerificationTypeInfo;
import edu.rit.csh.intraspect.data.constant.*;
import edu.rit.csh.intraspect.data.instruction.Instruction;
import edu.rit.csh.intraspect.data.instruction.branch.*;
import edu.rit.csh.intraspect.data.instruction.constant.*;
import edu.rit.csh.intraspect.data.instruction.control.IReturnInstruction;
import edu.rit.csh.intraspect.data.instruction.control.ReturnInstruction;
import edu.rit.csh.intraspect.data.instruction.field.GetStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.field.PutStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.invoke.InvokeStaticInstruction;
import edu.rit.csh.intraspect.data.instruction.invoke.InvokeVirtualInstruction;
import edu.rit.csh.intraspect.data.instruction.load.IALoadInstruction;
import edu.rit.csh.intraspect.data.instruction.load.ILoad_0Instruction;
import edu.rit.csh.intraspect.data.instruction.math.add.IAddInstruction;
import edu.rit.csh.intraspect.data.instruction.math.rem.IRemInstruction;
import edu.rit.csh.intraspect.data.instruction.object.NewArrayInstruction;
import edu.rit.csh.intraspect.data.instruction.stack.DupInstruction;
import edu.rit.csh.intraspect.data.instruction.stack.PopInstruction;
import edu.rit.csh.intraspect.data.instruction.store.IAStoreInstruction;
import edu.rit.csh.intraspect.util.OffsetOutputStream;

import java.io.*;
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
                throw new IOError(e);
            }
        });
        return (int) out.getCounter();
    }

    public static void main(String[] args) throws IOException {
        File outFile = null;
        File inFile = null;
        int dataSize = 10_000;
        boolean verbose = false;
        try {
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                switch (arg) {
                    case "-o", "--output" -> {
                        String path = args[++i];
                        if (!path.endsWith(".class")) {
                            path += ".class";
                        }
                        outFile = new File(path);
                        if (!outFile.exists()) {
                            outFile.createNewFile();
                        }
                        if (!outFile.canWrite()) {
                            System.out.println("Cannot write to file");
                            return;
                        }
                    }
                    case "-i", "--input" -> {
                        final String path = args[++i];
                        inFile = new File(path);
                        if (!inFile.exists()) {
                            System.out.println("File '" + path + "' not found");
                            return;
                        }
                        if (!inFile.canRead()) {
                            System.out.println("Cannot read from file");
                            return;
                        }
                    }
                    case "-d", "--data-size" -> dataSize = Integer.parseInt(args[i++]);
                    case "-v", "--verbose" -> verbose = true;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("Missing parameter for " + args[args.length - 1]);
            return;
        }
        if (outFile == null) {
            outFile = new File("a.class");
        }
        final String className = outFile.getName().substring(0, outFile.getName().lastIndexOf(".class"));
        ClassFile cf = new ClassFile();
        cf.setThisClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent(className))));
        cf.setSuperClass(cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/Object"))));
        cf.setFlag(ClassFile.AccessFlag.PUBLIC);
        cf.setMajorVersion(new MajorVersion(61));
        cf.setMinorVersion(0);
        if (inFile != null) {
            cf.addAttribute(new SourceFileAttribute(
                    cf.putUTFIfAbsent("SourceFile"),
                    cf.putUTFIfAbsent(inFile.getName())
            ));
        }

        final int systemClass = cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/lang/System")));
        final int printStreamClass = cf.addConstant(new ClassConstant(cf.putUTFIfAbsent("java/io/PrintStream")));
        final int soutField = cf.addConstant(new FieldRefConstant(
                systemClass,
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("out"),
                        cf.putUTFIfAbsent("Ljava/io/PrintStream;")
                ))
        ));
        final int printlnMethod = cf.addConstant(new MethodRefConstant(
                printStreamClass,
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("println"),
                        cf.putUTFIfAbsent("(Ljava/lang/String;)V")
                ))
        ));
        final int sysExitMethod = cf.addConstant(new MethodRefConstant(
                systemClass,
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("exit"),
                        cf.putUTFIfAbsent("(I)V")
                ))
        ));

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
                cf.putUTFIfAbsent("[I"),
                new AttributeDesc[0]
        ));

        final int dataField = cf.addConstant(new FieldRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("data"),
                        cf.putUTFIfAbsent("[I")
                ))
        ));


        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("get"),
                cf.putUTFIfAbsent("()I"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                0,
                                new Instruction[]{
                                        new GetStaticInstruction(dataField),
                                        new GetStaticInstruction(pointerField),
                                        new IALoadInstruction(),
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
                        cf.putUTFIfAbsent("()I")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("put"),
                cf.putUTFIfAbsent("(I)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                3,
                                1,
                                new Instruction[]{
                                        new GetStaticInstruction(dataField),
                                        new GetStaticInstruction(pointerField),
                                        new ILoad_0Instruction(),
                                        new IAStoreInstruction(),
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
                        cf.putUTFIfAbsent("(I)V")
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
                                        new GetStaticInstruction(soutField),
                                        new InvokeStaticInstruction(getMethod),
                                        new InvokeVirtualInstruction(
                                                cf.addConstant(new MethodRefConstant(
                                                        printStreamClass,
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
                                        new IAStoreInstruction(),
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
                cf.putUTFIfAbsent("(I)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                2,
                                1,
                                new Instruction[]{
                                        new InvokeStaticInstruction(getMethod),
                                        new ILoad_0Instruction(),
                                        new IAddInstruction(),
                                        new DupInstruction(),
                                        new IfgeInstruction(10),
                                        new SipushInstruction((short) 256),
                                        new IAddInstruction(),
                                        new GotoInstruction(-8),
                                        new SipushInstruction((short) 256),
                                        new IRemInstruction(),
                                        new InvokeStaticInstruction(setMethod),
                                        new ReturnInstruction()
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[]{
                                        new StackMapTableAttribute(
                                                cf.putUTFIfAbsent("StackMapTable"),
                                                new StackMapFrame[]{
                                                        new FullFrame(
                                                                5,
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                },
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo(),
                                                                }
                                                        ),
                                                        new FullFrame(
                                                                10,
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                },
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo(),
                                                                }
                                                        ),
                                                }
                                        )
                                }
                        )
                }
        ));

        final int plusMinusMethod = cf.addConstant(new MethodRefConstant(
                cf.getThisClassIndex(),
                cf.addConstant(new NameAndTypeConstant(
                        cf.putUTFIfAbsent("+\u0007-"),
                        cf.putUTFIfAbsent("(I)V")
                ))
        ));

        cf.addMethod(new MethodDesc(
                MethodDesc.combineFlags(MethodDesc.AccessFlag.PRIVATE, MethodDesc.AccessFlag.STATIC),
                cf.putUTFIfAbsent("shift"),
                cf.putUTFIfAbsent("(I)V"),
                new AttributeDesc[]{
                        new CodeAttribute(
                                cf.putUTFIfAbsent("Code"),
                                3,
                                1,
                                new Instruction[]{
                                        new GetStaticInstruction(pointerField),
                                        new ILoad_0Instruction(),
                                        new IAddInstruction(),
                                        new DupInstruction(),
                                        new IfltInstruction(13),
                                        new DupInstruction(),
                                        new LdcInstruction(cf.addConstant(new IntegerConstant(dataSize))),
                                        new If_icmpgeInstruction(21),
                                        new PutStaticInstruction(pointerField),
                                        new ReturnInstruction(),
                                        new PopInstruction(),
                                        new GetStaticInstruction(soutField),
                                        new LdcInstruction(cf.addConstant(new StringConstant(cf.putUTFIfAbsent("ERROR: Illegal negative pointer")))),
                                        new InvokeVirtualInstruction(printlnMethod),
                                        new IConst_1Instruction(),
                                        new InvokeStaticInstruction(sysExitMethod),
                                        new ReturnInstruction(),
                                        new PopInstruction(),
                                        new GetStaticInstruction(soutField),
                                        new LdcInstruction(cf.addConstant(new StringConstant(cf.putUTFIfAbsent("ERROR: Pointer too large")))),
                                        new InvokeVirtualInstruction(printlnMethod),
                                        new IConst_1Instruction(),
                                        new InvokeStaticInstruction(sysExitMethod),
                                        new ReturnInstruction(),
                                },
                                new CodeAttribute.ExceptionDesc[0],
                                new AttributeDesc[]{
                                        new StackMapTableAttribute(
                                                cf.putUTFIfAbsent("StackMapTable"),
                                                new StackMapFrame[]{
                                                        new FullFrame(
                                                                19,
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                },
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                }
                                                        ),
                                                        new FullFrame(
                                                                13,
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                },
                                                                new VerificationTypeInfo[]{
                                                                        new IntegerVariableInfo()
                                                                }
                                                        )
                                                }
                                        )
                                }
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

        Stack<Integer> openBracketPositions = new Stack<>();
        List<Integer> allJumpPositions = new ArrayList<>();

        List<Instruction> mainMethodCode = new ArrayList<>(List.of(
                new IConst_0Instruction(),
                new PutStaticInstruction(pointerField),
                new SipushInstruction((short) dataSize),
                new NewArrayInstruction(10),
                new PutStaticInstruction(dataField)
        ));

        InputStream in = null;
        if (inFile == null) {
            in = System.in;
        } else {
            in = new FileInputStream(inFile);
        }

        int read;

        int plusMinus = 0;
        int shift = 0;

        while ((read = in.read()) != -1) {
            final char c = (char) read;
            if (c == '+') {
                plusMinus++;
            } else if (c == '-') {
                plusMinus--;
            } else if (plusMinus != 0) {
                while (plusMinus < 0) {
                    plusMinus += 256;
                }
                while (plusMinus >= 256) {
                    plusMinus -= 256;
                }
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
                if (openBracketPositions.isEmpty()) {
                    System.out.println("Missing open bracket");
                    return;
                }
                final int jumpPos = openBracketPositions.pop();
                mainMethodCode.add(new GotoInstruction(jumpPos - getIndex(mainMethodCode)));
                mainMethodCode.set(mainMethodCode.lastIndexOf(null), new IfeqInstruction(getIndex(mainMethodCode) - jumpPos - 3));
                allJumpPositions.add(getIndex(mainMethodCode));
            }
        }

        if (!openBracketPositions.isEmpty()) {
            System.out.println("Missing close bracket");
            return;
        }

        mainMethodCode.add(new ReturnInstruction());

        List<StackMapFrame> stackMapFrames = new ArrayList<>();

        int lastPosition = -1;
        for (int position : allJumpPositions) {
            if (position == lastPosition) {
                continue;
            }
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

        cf.write(new FileOutputStream(outFile));
    }
}
