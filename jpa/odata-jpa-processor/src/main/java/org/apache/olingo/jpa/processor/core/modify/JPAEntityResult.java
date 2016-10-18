package org.apache.olingo.jpa.processor.core.modify;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADescriptionAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAAssociationPath;
import org.apache.olingo.jpa.processor.core.query.ExpressionUtil;
import org.apache.org.jpa.processor.core.converter.JPAExpandResult;

public class JPAEntityResult implements JPAExpandResult {
  private final Object jpaEntity;
  private final JPAEntityType et;
  private final Map<JPAAssociationPath, JPAExpandResult> children;
  private final List<JPAPath> pathList;
  private final List<Tuple> result;
  private final Locale locale;

  public JPAEntityResult(JPAEntityType et, Object jpaEntity, Map<String, List<String>> requestHeaders)
      throws ODataJPAModelException {
    this.jpaEntity = jpaEntity;
    this.et = et;
    this.children = new HashMap<JPAAssociationPath, JPAExpandResult>(0);
    this.pathList = et.getPathList();
    this.locale = ExpressionUtil.determineLocale(requestHeaders);
    this.result = createResult();
  }

  @Override
  public Map<JPAAssociationPath, JPAExpandResult> getChildren() {
    return children;
  }

  @Override
  public Integer getCount() {
    return null;
  }

  @Override
  public JPAEntityType getEntityType() {
    return et;
  }

  @Override
  public List<Tuple> getResult(String key) {
    return result;
  }

  @Override
  public boolean hasCount() {
    return false;
  }

  private Map<String, Object> buildGetterMap(Object instance) {

    final Map<String, Object> getterMap = new HashMap<String, Object>();
    // TODO Performance: Don't recreate the complete getter list over and over again
    Method[] methods = instance.getClass().getMethods();
    for (Method meth : methods) {
      if (meth.getName().substring(0, 3).equals("get")) {
        String attributeName = meth.getName().substring(3, 4).toLowerCase() + meth.getName().substring(4);
        try {
          Object value = meth.invoke(instance);
          getterMap.put(attributeName, value);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (IllegalArgumentException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        }
      }
    }
    return getterMap;
  }

  private void convertPathToTuple(final JPATuple tuple, final Map<String, Object> getterMap, final JPAPath path,
      final int index) {

    Object value = getterMap.get(path.getPath().get(index).getInternalName());
    if (path.getPath().size() == index + 1 || value == null) {
      if (path.getPath().get(index) instanceof JPADescriptionAttribute) {
        Collection<Object> desc = (Collection<Object>) value;
        if (desc != null) {

          for (Object entry : desc) {
            final Map<String, Object> descGetterMap = buildGetterMap(entry);
            JPADescriptionAttribute jpaAttribute = (JPADescriptionAttribute) path.getPath().get(index);
            value = descGetterMap.get(jpaAttribute.getLocaleFieldName().getPath().get(0).getInternalName());
            if (locale.getLanguage().equals(value)
                || locale.toString().equals(value)) {
              tuple.addElement(path.getAlias(), path.getLeaf().getType(), value);
              break;
            }
          }
        } else
          tuple.addElement(path.getAlias(), path.getLeaf().getType(), null);
      } else {
        tuple.addElement(path.getAlias(), path.getLeaf().getType(), value);
      }
    } else {
      final Map<String, Object> embeddedGetterMap = buildGetterMap(value);
      convertPathToTuple(tuple, embeddedGetterMap, path, index + 1);
    }
  }

  private List<Tuple> createResult() {
    JPATuple tuple = new JPATuple();
    List<Tuple> tupleResult = new ArrayList<Tuple>();

    final Map<String, Object> getterMap = buildGetterMap(jpaEntity);
    for (JPAPath path : pathList) {
      convertPathToTuple(tuple, getterMap, path, 0);
    }

    tupleResult.add(tuple);
    return tupleResult;
  }

  private class JPATuple implements Tuple {
    private List<TupleElement<?>> elements = new ArrayList<TupleElement<?>>();
    private Map<String, Object> values = new HashMap<String, Object>();

    public void addElement(String alias, Class<?> javaType, Object value) {
      elements.add(new JPATupleElement<Object>(alias, javaType));
      values.put(alias, value);

    }

    @Override
    public Object get(int arg0) {
      assert 1 == 2;
      return null;
    }

    @Override
    public <X> X get(int arg0, Class<X> arg1) {
      assert 1 == 2;
      return null;
    }

    /**
     * Get the value of the tuple element to which the
     * specified alias has been assigned.
     * @param alias alias assigned to tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException if alias
     * does not correspond to an element in the
     * query result tuple
     */
    @Override
    public Object get(String alias) {
      return values.get(alias);
    }

    @Override
    public <X> X get(String arg0, Class<X> arg1) {
      assert 1 == 2;
      return null;
    }

    @Override
    public <X> X get(TupleElement<X> arg0) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public List<TupleElement<?>> getElements() {
      return elements;
    }

    @Override
    public Object[] toArray() {
      return null;
    }

  }

  private class JPATupleElement<X> implements TupleElement<X> {

    private final String alias;
    private final Class<? extends X> javaType;

    public JPATupleElement(String alias, Class<? extends X> javaType) {
      this.alias = alias;
      this.javaType = javaType;
    }

    @Override
    public String getAlias() {
      return alias;
    }

    @Override
    public Class<? extends X> getJavaType() {
      return javaType;
    }

  }

}