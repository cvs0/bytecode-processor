package io.github.cvs0.bytecode.plugin;

import io.github.cvs0.bytecode.JarMapping;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {

    private PluginManager pluginManager;
    private TestPlugin testPlugin;

    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager();
        testPlugin = new TestPlugin();
    }

    @Test
    void testRegisterPlugin() {
        pluginManager.registerPlugin(testPlugin);

        assertEquals(1, pluginManager.getPluginCount());
        assertTrue(pluginManager.hasPlugin("TestPlugin"));
        assertEquals(testPlugin, pluginManager.getPlugin("TestPlugin"));
    }

    @Test
    void testRegisterNullPlugin() {
        assertThrows(IllegalArgumentException.class, () -> {
            pluginManager.registerPlugin(null);
        });
    }

    @Test
    void testRegisterDuplicatePlugin() {
        pluginManager.registerPlugin(testPlugin);

        TestPlugin duplicate = new TestPlugin();
        assertThrows(IllegalArgumentException.class, () -> {
            pluginManager.registerPlugin(duplicate);
        });
    }

    @Test
    void testUnregisterPlugin() {
        pluginManager.registerPlugin(testPlugin);
        assertTrue(pluginManager.hasPlugin("TestPlugin"));

        pluginManager.unregisterPlugin("TestPlugin");

        assertFalse(pluginManager.hasPlugin("TestPlugin"));
        assertEquals(0, pluginManager.getPluginCount());
        assertTrue(testPlugin.isCleanedUp());
    }

    @Test
    void testGetEnabledPlugins() {
        TestPlugin plugin1 = new TestPlugin("Plugin1");
        TestPlugin plugin2 = new TestPlugin("Plugin2");
        plugin2.setEnabled(false);

        pluginManager.registerPlugin(plugin1);
        pluginManager.registerPlugin(plugin2);

        assertEquals(2, pluginManager.getPluginCount());
        assertEquals(1, pluginManager.getEnabledPluginCount());
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getEnabledPlugins().contains(plugin1));
        assertFalse(pluginManager.getEnabledPlugins().contains(plugin2));
    }

    @Test
    void testInitializePlugins() {
        pluginManager.registerPlugin(testPlugin);
        assertFalse(testPlugin.isInitialized());

        pluginManager.initializePlugins();

        assertTrue(testPlugin.isInitialized());
    }

    @Test
    void testProcessWithPluginsReturnsFalseWhenProcessThrows() {
        Logger log = Logger.getLogger(PluginManager.class.getName());
        Level saved = log.getLevel();
        log.setLevel(Level.OFF);
        try {
            AbstractPlugin flaky = new AbstractPlugin("Flaky", "1", "x") {
                @Override
                public void process(JarMapping mapping) {
                    throw new RuntimeException("process boom");
                }
            };
            pluginManager.registerPlugin(flaky);
            assertFalse(pluginManager.processWithPlugins(new JarMapping("x.jar")));
        } finally {
            log.setLevel(saved);
        }
    }

    @Test
    void testInitializePluginsFailsWhenPluginThrows() {
        AbstractPlugin bad = new AbstractPlugin("Bad", "1", "x") {
            @Override
            public void process(JarMapping mapping) { }

            @Override
            public void initialize() {
                throw new RuntimeException("boom");
            }
        };
        pluginManager.registerPlugin(bad);
        assertThrows(IllegalStateException.class, () -> pluginManager.initializePlugins());
    }

    @Test
    void testProcessWithPlugins() {
        pluginManager.registerPlugin(testPlugin);
        JarMapping mapping = new JarMapping("test.jar");

        assertTrue(pluginManager.processWithPlugins(mapping));

        assertTrue(testPlugin.isInitialized());
        assertTrue(testPlugin.isProcessed());
    }

    @Test
    void testCleanupPlugins() {
        pluginManager.registerPlugin(testPlugin);
        pluginManager.initializePlugins();

        pluginManager.cleanupPlugins();

        assertTrue(testPlugin.isCleanedUp());
    }

    @Test
    void testPluginPriority() {
        TestPlugin lowPriority = new TestPlugin("Low", 1);
        TestPlugin highPriority = new TestPlugin("High", 10);

        pluginManager.registerPlugin(lowPriority);
        pluginManager.registerPlugin(highPriority);

        var enabledPlugins = pluginManager.getEnabledPlugins();
        assertEquals(highPriority, enabledPlugins.get(0));
        assertEquals(lowPriority, enabledPlugins.get(1));
    }

    @Test
    void testEnableDisablePlugin() {
        pluginManager.registerPlugin(testPlugin);
        assertTrue(testPlugin.isEnabled());

        pluginManager.disablePlugin("TestPlugin");
        assertFalse(testPlugin.isEnabled());

        pluginManager.enablePlugin("TestPlugin");
        assertTrue(testPlugin.isEnabled());
    }

    @Test
    void testClear() {
        pluginManager.registerPlugin(testPlugin);
        pluginManager.initializePlugins();

        pluginManager.clear();

        assertEquals(0, pluginManager.getPluginCount());
        assertTrue(testPlugin.isCleanedUp());
    }

    private static class TestPlugin extends AbstractPlugin {
        @Getter
        private boolean initialized = false;
        @Getter
        private boolean processed = false;
        @Getter
        private boolean cleanedUp = false;

        public TestPlugin() {
            super("TestPlugin", "1.0.0", "Test plugin for unit tests");
        }

        public TestPlugin(String name) {
            super(name, "1.0.0", "Test plugin for unit tests");
        }

        public TestPlugin(String name, int priority) {
            super(name, "1.0.0", "Test plugin for unit tests");
        }

        @Override
        public void initialize() {
            super.initialize();
            initialized = true;
        }

        @Override
        public void process(JarMapping mapping) {
            processed = true;
        }

        @Override
        public void cleanup() {
            super.cleanup();
            cleanedUp = true;
        }

        @Override
        public int getPriority() {
            return getIntConfig("priority", 0);
        }
    }
}
