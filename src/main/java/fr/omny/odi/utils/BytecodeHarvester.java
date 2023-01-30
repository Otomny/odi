package fr.omny.odi.utils;


import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import lombok.Getter;

@Getter
public class BytecodeHarvester extends ClassVisitor {

	private String superClass;
	private String className;
	private List<String> annotations = new ArrayList<>();
	private List<String> interfaces = new ArrayList<>();

	protected BytecodeHarvester(String className) {
		super(Opcodes.ASM8);
		this.className = className;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		String a = descriptor.substring(1);
		String b = a.substring(0, a.length() - 1);
		this.annotations.add(b);
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		super.visit(version, access, name, signature, superName, interfaces);
		this.superClass = superName;
		for (String anInterface : interfaces) {
			this.interfaces.add(anInterface);
		}
	}

}
