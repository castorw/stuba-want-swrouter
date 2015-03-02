package net.ctrdn.stuba.want.swrouter.api;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.ctrdn.stuba.want.swrouter.core.RouterController;
import net.ctrdn.stuba.want.swrouter.exception.APIException;
import net.ctrdn.stuba.want.swrouter.exception.APIRegistryException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIMethodRegistry {

    private final Logger logger = LoggerFactory.getLogger(APIMethodRegistry.class);
    private static APIMethodRegistry registry;
    private static RouterController routerController;
    private final Map<String, Class<? extends APIMethod>> methodClassMap = new ConcurrentHashMap<>();

    private APIMethodRegistry() throws APIException {
        Reflections reflections = new Reflections("net.ctrdn");
        try {
            for (Class<?> foundClass : reflections.getSubTypesOf(APIMethod.class)) {
                if (!Modifier.isAbstract(foundClass.getModifiers())) {
                    Constructor constructor = foundClass.getConstructor(RouterController.class);
                    APIMethod instance = (APIMethod) constructor.newInstance(APIMethodRegistry.routerController);
                    String path = instance.getPath();
                    methodClassMap.put(path, (Class<? extends APIMethod>) foundClass);
                    this.logger.trace("Adding API method " + path);
                }
            }
            this.logger.debug("Populated " + this.methodClassMap.size() + " API methods");
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            throw new APIException("Error populating API registry", ex);
        }
    }

    public APIMethod getMethod(String path) throws APIRegistryException {
        if (this.methodClassMap.containsKey(path)) {
            try {
                Class<? extends APIMethod> clz = this.methodClassMap.get(path);
                Constructor ctr = clz.getConstructor(RouterController.class);
                APIMethod inst = (APIMethod) ctr.newInstance(APIMethodRegistry.routerController);
                return inst;
            } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                throw new APIRegistryException("Unable to instantinate API method", ex);
            }
        }
        return null;
    }

    public String[] getMethodNames() {
        return this.methodClassMap.keySet().toArray(new String[this.methodClassMap.keySet().size()]);
    }

    public static void initalize(RouterController proxyController) throws APIException {
        APIMethodRegistry.registry = new APIMethodRegistry();
        APIMethodRegistry.routerController = proxyController;
    }

    public static APIMethodRegistry getInstance() {
        return APIMethodRegistry.registry;
    }
}
