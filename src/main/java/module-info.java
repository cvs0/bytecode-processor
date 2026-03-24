module bytecode.processor {
    requires java.logging;
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires info.picocli;
    requires static lombok;

    exports io.github.cvs0.bytecode;
    exports io.github.cvs0.bytecode.attribute;
    exports io.github.cvs0.bytecode.member;
    exports io.github.cvs0.bytecode.analysis;
    exports io.github.cvs0.bytecode.util;
    exports io.github.cvs0.bytecode.clazz;
    exports io.github.cvs0.bytecode.instruction;
    exports io.github.cvs0.bytecode.plugin;
    exports io.github.cvs0.bytecode.test;
    exports io.github.cvs0.bytecode.transform;

    opens io.github.cvs0.bytecode.cli to info.picocli;
}
