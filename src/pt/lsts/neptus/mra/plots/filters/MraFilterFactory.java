package pt.lsts.neptus.mra.plots.filters;

import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Set;

public class MraFilterFactory {
    private static final HashMap<String, String> availableFilters = fetchAvailableFilters();

    private static HashMap<String, String> fetchAvailableFilters() {
        HashMap<String, String> filters = new HashMap<>();

        for (String pkg : new String[] {"pt.lsts.neptus.mra.plots.filters"}) {
            Reflections reflections = new Reflections(pkg);
            reflections.getTypesAnnotatedWith(MraFilterDescription.class)
                    .forEach(c -> filters.put(c.getAnnotation(MraFilterDescription.class).name(), c.getName()));
        }

        return filters;
    }

    /**
     * Returns the names of all the available filters
     * */
    public static Set<String> getAvailableFilters() {
        return availableFilters.keySet();
    }

    /**
     * Returns an instance of the filter with the given name,
     * or null if some error occurs.
     * */
    public static MraFilter getInstanceOf(String filterName) {
        try {
            return (MraFilter) Class.forName(availableFilters.get(filterName)).getConstructor().newInstance();
        } catch (InstantiationException |
                IllegalAccessException |
                InvocationTargetException |
                NoSuchMethodException |
                ClassNotFoundException e) {
            e.printStackTrace();

            return null;
        }
    }
}
