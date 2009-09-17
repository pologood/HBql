package org.apache.hadoop.hbase.hbql.query.schema;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.hbql.client.HColumn;
import org.apache.hadoop.hbase.hbql.client.HColumnVersionMap;
import org.apache.hadoop.hbase.hbql.client.HFamily;
import org.apache.hadoop.hbase.hbql.client.HPersistException;
import org.apache.hadoop.hbase.hbql.client.HTable;
import org.apache.hadoop.hbase.hbql.query.io.Serialization;
import org.apache.hadoop.hbase.hbql.query.util.Lists;
import org.apache.hadoop.hbase.hbql.query.util.Maps;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Sep 9, 2009
 * Time: 9:47:01 AM
 */
public class AnnotationSchema extends HBaseSchema {

    private final static Map<Class<?>, AnnotationSchema> annotationSchemaMap = Maps.newHashMap();
    private final static Map<String, Class<?>> classCacheMap = Maps.newHashMap();

    private final Map<String, List<ColumnAttrib>> columnAttribListByFamilyNameMap = Maps.newHashMap();

    private final Class<?> clazz;
    private final HTable table;
    private final HFamily[] families;

    private AnnotationSchema(final Class clazz) throws HPersistException {

        this.clazz = clazz;

        // Make sure there is an empty constructor declared
        try {
            this.getClazz().getConstructor();
        }
        catch (NoSuchMethodException e) {
            throw new HPersistException("Class " + this + " is missing a null constructor");
        }

        this.table = this.getClazz().getAnnotation(HTable.class);

        if (this.table == null)
            throw new HPersistException("Class " + this + " is missing @HTable annotation");

        this.families = this.table.families();

        if (this.families == null)
            throw new HPersistException("Class " + this + " is missing @HFamily values in @HTable annotation");

        for (final HFamily family : families) {
            final List<ColumnAttrib> attribs = Lists.newArrayList();
            this.setColumnAttribListByFamilyName(family.name(), attribs);
        }

        // First process all HColumn fields so we can do lookup from HColumnVersionMaps
        for (final Field field : this.getClazz().getDeclaredFields())
            if (field.getAnnotation(HColumn.class) != null)
                this.processColumnAnnotation(field);

        if (this.getKeyAttrib() == null)
            throw new HPersistException("Class " + this + " is missing an instance variable "
                                        + "annotated with @HColumn(key=true)");

        for (final Field field : this.getClazz().getDeclaredFields())
            if (field.getAnnotation(HColumnVersionMap.class) != null)
                this.processColumnVersionAnnotation(field);
    }

    public synchronized static AnnotationSchema getAnnotationSchema(final String objName) throws HPersistException {

        // First see if already cached
        Class<?> clazz = getClassCacheMap().get(objName);

        if (clazz != null)
            return getAnnotationSchema(clazz);

        final String classpath = System.getProperty("java.class.path");
        for (final String val : classpath.split(":")) {
            try {
                if (val.toLowerCase().endsWith(".jar")) {
                    JarFile jarFile = new JarFile(val);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        final JarEntry entry = entries.nextElement();
                        final AnnotationSchema schema = searchPackage(entry.getName(), objName);
                        if (schema != null)
                            return schema;
                    }
                }

                final File rootdir = new File(val);
                if (rootdir.isDirectory())
                    return searchDirectory(rootdir, rootdir.getCanonicalPath(), "", objName + ".class");
            }
            catch (IOException e) {
                // Go to next entry
            }
        }

        return null;
    }

    private static AnnotationSchema searchDirectory(final File dir,
                                                    final String rootDir,
                                                    final String prefix,
                                                    final String dotClassName) throws HPersistException {

        final List<File> subdirs = Lists.newArrayList();
        final String[] contents = dir.list();

        for (final String elem : contents) {

            final File file = new File(dir + "/" + elem);
            if (file.isDirectory()) {
                subdirs.add(file);
            }
            else {
                final String pathname = file.getAbsolutePath().replace('/', '.');
                if (pathname.endsWith(dotClassName)) {
                    final String rootprefix = rootDir.replace('/', '.');
                    final String pathsuffix = pathname.substring(rootprefix.length() + 1);
                    return setClassCache(stripDotClass(pathsuffix), stripDotClass(dotClassName));
                }
            }
        }

        // Now search the dirs
        for (final File subdir : subdirs) {
            final String nextdir = (prefix.length() == 0) ? subdir.getName() : prefix + "." + subdir.getName();
            final AnnotationSchema schema = searchDirectory(subdir, rootDir, nextdir, dotClassName);
            if (schema != null)
                return schema;
        }

        return null;
    }

    private static String stripDotClass(final String str) {
        return str.substring(0, str.length() - ".class".length());
    }

    private static AnnotationSchema searchPackage(final String pkgName, final String objName) throws HPersistException {

        if (pkgName == null)
            return null;

        final String prefix = pkgName.replaceAll("/", ".");

        if (prefix.startsWith("META-INF.")
            || prefix.startsWith("antlr.")
            || prefix.startsWith("org.antlr.")
            || prefix.startsWith("apple.")
            || prefix.startsWith("com.apple.")
            || prefix.startsWith("sun.")
            || prefix.startsWith("com.sun.")
            || prefix.startsWith("java.")
            || prefix.startsWith("javax.")
            || prefix.startsWith("org.apache.zookeeper.")
            || prefix.startsWith("org.apache.bookkeeper.")
            || prefix.startsWith("org.apache.jute.")
            || prefix.startsWith("com.google.common.")
            || prefix.startsWith("org.apache.log4j.")
            || prefix.startsWith("junit.")
            || prefix.startsWith("org.junit.")
            || prefix.startsWith("org.xml.")
            || prefix.startsWith("org.w3c.")
            || prefix.startsWith("org.omg.")
            || prefix.startsWith("org.apache.mina.")
            || prefix.startsWith("org.apache.hadoop.")
            || prefix.startsWith("org.apache.commons.logging.")
            || prefix.startsWith("org.jcp.")
            || prefix.startsWith("org.slf4j.")
            || prefix.startsWith("org.ietf.")
            || prefix.startsWith("org.relaxng.")
            || prefix.startsWith("netscape.")
                )
            return null;

        final String fullname = prefix
                                + ((!prefix.endsWith(".") && prefix.length() > 0) ? "." : "")
                                + objName;

        return setClassCache(fullname, objName);
    }

    private static AnnotationSchema setClassCache(final String name, final String objName) throws HPersistException {

        final Class<?> clazz = getClass(name);
        if (clazz == null) {
            return null;
        }
        else {
            getClassCacheMap().put(objName, clazz);
            return getAnnotationSchema(clazz);
        }
    }

    private static Class getClass(final String str) throws HPersistException {
        try {
            final Class<?> clazz = Class.forName(str);

            // Make sure inner class is static
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
                final String s = "Inner class " + clazz.getName() + " must be declared static";
                System.err.println(s);
                throw new HPersistException(s);
            }

            return clazz;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static AnnotationSchema getAnnotationSchema(final Object obj) throws HPersistException {
        return getAnnotationSchema(obj.getClass());
    }

    public synchronized static AnnotationSchema getAnnotationSchema(final Class<?> clazz) throws HPersistException {

        AnnotationSchema schema = getAnnotationSchemaMap().get(clazz);
        if (schema != null)
            return schema;

        schema = new AnnotationSchema(clazz);
        getAnnotationSchemaMap().put(clazz, schema);
        return schema;
    }

    private static Map<Class<?>, AnnotationSchema> getAnnotationSchemaMap() {
        return annotationSchemaMap;
    }

    private static Map<String, Class<?>> getClassCacheMap() {
        return classCacheMap;
    }

    @Override
    public String toString() {
        return this.getSchemaName();
    }

    @Override
    public String getSchemaName() {
        return this.getClazz().getName();
    }

    public Class<?> getClazz() {
        return this.clazz;
    }

    public HFamily[] getFamilies() {
        return this.families;
    }

    private void processColumnAnnotation(final Field field) throws HPersistException {

        final ColumnAttrib attrib = new CurrentValueAttrib(field);

        this.addVariableAttrib(attrib);
        this.addColumnAttrib(attrib);

        if (attrib.isKeyAttrib()) {
            if (this.getKeyAttrib() != null)
                throw new HPersistException("Class " + this + " has multiple instance variables "
                                            + "annotated with @HColumn(key=true)");

            this.setKeyAttrib(attrib);
        }
        else {
            final String family = attrib.getFamilyName();

            if (family.length() == 0)
                throw new HPersistException(attrib.getObjectQualifiedName()
                                            + " is missing family name in annotation");

            if (!this.containsFamilyName(family))
                throw new HPersistException(attrib.getObjectQualifiedName()
                                            + " references unknown family: " + family);

            this.getColumnAttribListByFamilyName(family).add(attrib);
        }

    }

    private void processColumnVersionAnnotation(final Field field) throws HPersistException {
        final VersionAttrib attrib = VersionAttrib.newVersionAttrib(this, field);
        this.addVariableAttrib(attrib);
        this.addVersionAttrib(attrib);
    }

    public String getTableName() {
        final String tableName = this.table.name();
        return (tableName.length() > 0) ? tableName : clazz.getSimpleName();
    }

    @Override
    public Object newInstance() throws IllegalAccessException, InstantiationException {
        return this.getClazz().newInstance();
    }

    // *** columnAttribListByFamilyNameMap
    private Map<String, List<ColumnAttrib>> getColumnAttribListByFamilyNameMap() {
        return columnAttribListByFamilyNameMap;
    }

    public Set<String> getFamilyNameList() {
        return this.getColumnAttribListByFamilyNameMap().keySet();
    }

    public List<ColumnAttrib> getColumnAttribListByFamilyName(final String s) {
        return this.getColumnAttribListByFamilyNameMap().get(s);
    }

    private boolean containsFamilyName(final String s) {
        return this.getColumnAttribListByFamilyNameMap().containsKey(s);
    }

    public void setColumnAttribListByFamilyName(final String s,
                                                final List<ColumnAttrib> attribList) throws HPersistException {
        if (this.containsFamilyName(s))
            throw new HPersistException(s + " already delcared");
        this.getColumnAttribListByFamilyNameMap().put(s, attribList);
    }

    @Override
    public Object getObject(final Serialization ser,
                            final List<String> fieldList,
                            final int maxVersions,
                            final Result result) throws HPersistException {

        try {
            // Create object and assign key value
            final Object newobj = createNewObject(ser, result);

            // Assign most recent values
            assignCurrentValues(ser, fieldList, result, newobj);

            // Assign the versioned values
            if (maxVersions > 1)
                assignVersionedValues(ser, fieldList, result, newobj);

            return newobj;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new HPersistException("Error in getObject()");
        }
    }

    private Object createNewObject(final Serialization ser, final Result result) throws IOException, HPersistException {

        // Create new instance and set key value
        final ColumnAttrib keyattrib = this.getKeyAttrib();
        final Object newobj;
        try {
            newobj = this.newInstance();
            final byte[] keybytes = result.getRow();
            keyattrib.setCurrentValue(ser, newobj, 0, keybytes);
        }
        catch (InstantiationException e) {
            throw new RuntimeException("Cannot create new instance of " + this.getSchemaName());
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set value for key  " + keyattrib.getVariableName()
                                       + " for " + this.getSchemaName());
        }
        return newobj;
    }

    public List<VarDesc> getVarDescList() {
        final List<VarDesc> varList = Lists.newArrayList();
        for (final ColumnAttrib col : this.getColumnAttribByFamilyQualifiedColumnNameMap().values()) {
            final String coltype = (col.isKeyAttrib())
                                   ? FieldType.KeyType.getFirstSynonym()
                                   : col.getFieldType().getFirstSynonym();
            varList.add(VarDesc.newVarDesc(col.getVariableName(), col.getFamilyQualifiedName(), coltype));
        }
        return varList;
    }
}