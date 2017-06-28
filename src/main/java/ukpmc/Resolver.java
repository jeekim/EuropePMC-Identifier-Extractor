package ukpmc;

import ukpmc.scala.Resolvable;

/**
 * Created by jee on 27/06/17.
 */
public abstract class Resolver implements Resolvable {
    public abstract String prefixDOI(String s);

    String normalizeID(String db, String id) {
        int dotIndex;
        dotIndex = id.indexOf(".");
        if (dotIndex != -1 && !"doi".equals(db)) id = id.substring(0, dotIndex);
        if (id.endsWith(")")) id = id.substring(0, id.length() - 1);
        return id.toUpperCase();
    }

}
