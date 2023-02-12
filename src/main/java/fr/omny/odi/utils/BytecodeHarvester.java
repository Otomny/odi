package fr.omny.odi.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import lombok.Getter;

@Getter
public class BytecodeHarvester extends ClassVisitor {

	private String superClass;
	private String className;
	private List<String> interfaces = new ArrayList<>();
	private Map<String, Map<String, String>> annotationsDatas = new HashMap<>();

	protected BytecodeHarvester(String className) {
		super(Opcodes.ASM9);
		this.className = className;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		String a = descriptor.substring(1);
		String b = a.substring(0, a.length() - 1);
		return new AnnotationHarvester(b);
	}

	public class AnnotationHarvester extends AnnotationVisitor {

		private String annotationName;

		private AnnotationHarvester(String annotationName) {
			super(Opcodes.ASM9);
			this.annotationName = annotationName;
			BytecodeHarvester.this.annotationsDatas.put(annotationName, new HashMap<>());
		}

		@Override
		public void visit(String name, Object value) {
			BytecodeHarvester.this.annotationsDatas.get(annotationName).put(name, value.toString());
		}

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
