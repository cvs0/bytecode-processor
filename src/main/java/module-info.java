module bytecode.processor {
    requires org.objectweb.asm.tree;
    requires org.objectweb.asm.commons;
    requires info.picocli;
    requires static lombok;

    exports io.github.cvs0.bytecode;
    exports io.github.cvs0.bytecode.analysis;
    exports io.github.cvs0.bytecode.attribute;
    exports io.github.cvs0.bytecode.clazz;
    exports io.github.cvs0.bytecode.cli;
    exports io.github.cvs0.bytecode.instruction;
    exports io.github.cvs0.bytecode.io;
    exports io.github.cvs0.bytecode.log;
    exports io.github.cvs0.bytecode.member;
    exports io.github.cvs0.bytecode.plugin;
    exports io.github.cvs0.bytecode.plugin.impl;
    exports io.github.cvs0.bytecode.runtime;
    exports io.github.cvs0.bytecode.runtime.clazz;
    exports io.github.cvs0.bytecode.runtime.resource;
    exports io.github.cvs0.bytecode.runtime.url;
    exports io.github.cvs0.bytecode.transform;
    exports io.github.cvs0.bytecode.transform.patcher;
    exports io.github.cvs0.bytecode.transform.remapper;
    exports io.github.cvs0.bytecode.transform.transformer;
    exports io.github.cvs0.bytecode.util;

    opens io.github.cvs0.bytecode.cli to info.picocli;
}
