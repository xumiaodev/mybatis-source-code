/**
 *    Copyright 2009-2016 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Clinton Begin
 */
public class CacheKey implements Cloneable, Serializable {

  private static final long serialVersionUID = 1146682552656046210L;

  public static final CacheKey NULL_CACHE_KEY = new NullCacheKey();

  private static final int DEFAULT_MULTIPLYER = 37;
  private static final int DEFAULT_HASHCODE = 17;

  private int multiplier; //参与计算hashcode，默认值 37
  private int hashcode; // 当前对象的hashcode，默认是 17
  private long checksum; // 校验和
  private int count; // updateList 中数量
  private List<Object> updateList; // 由该集合中所有的对象共同决定两个CacheKey是否相同

  public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<Object>();
  }

  public CacheKey(Object[] objects) {
    this();
    updateAll(objects);
  }

  public int getUpdateCount() {
    return updateList.size();
  }

  // 向 updateList中添加对象
  public void update(Object object) {
    if (object != null && object.getClass().isArray()) { // object 是 数组类型
      int length = Array.getLength(object); // 获取数组长度
      for (int i = 0; i < length; i++) {
        Object element = Array.get(object, i); // 获取每一个元素
        doUpdate(element);
      }
    } else {
      doUpdate(object);
    }
  }

  private void doUpdate(Object object) {
    int baseHashCode = object == null ? 1 : object.hashCode(); // 获取 object hashCode

    // 重新计算hashcode
    count++;
    checksum += baseHashCode;
    baseHashCode *= count;

    hashcode = multiplier * hashcode + baseHashCode;

    updateList.add(object); // 添加到list中
  }

  public void updateAll(Object[] objects) {
    for (Object o : objects) {
      update(o);
    }
  }

  // 比较两个CacheKey是否相等
  @Override
  public boolean equals(Object object) {
    if (this == object) { // 是否是同一对象
      return true;
    }
    if (!(object instanceof CacheKey)) { // 是否类型相同
      return false;
    }

    final CacheKey cacheKey = (CacheKey) object;

    if (hashcode != cacheKey.hashcode) { // 比较hashcode
      return false;
    }
    if (checksum != cacheKey.checksum) { // 比较校验和
      return false;
    }
    if (count != cacheKey.count) { // 比较count
      return false;
    }

    for (int i = 0; i < updateList.size(); i++) { // 比较集合中的每一项
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (thisObject == null) {
        if (thatObject != null) {
          return false;
        }
      } else {
        if (!thisObject.equals(thatObject)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public String toString() {
    StringBuilder returnValue = new StringBuilder().append(hashcode).append(':').append(checksum);
    for (Object object : updateList) {
      returnValue.append(':').append(object);
    }

    return returnValue.toString();
  }

  @Override
  public CacheKey clone() throws CloneNotSupportedException {
    CacheKey clonedCacheKey = (CacheKey) super.clone();
    clonedCacheKey.updateList = new ArrayList<Object>(updateList);
    return clonedCacheKey;
  }

}
