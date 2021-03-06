package org.poormanscastle.products.hit2assext.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

class RenderSessionContextImpl implements RenderSessionContext {

    private final static Logger logger = Logger.getLogger(RenderSessionContextImpl.class);

    private final Map<String, List<Object>> listMap = new HashMap<>();

    private final Map<String, Object> scalarMap = new HashMap<>();

    private int xmlSequence = 1;

    private int lastQueriedXmlSequence;

    /**
     * remember when this session item was created. If the clean up does not work for some reason
     * old sessions can be identified as obsolete by their age (e.g. older than 20s) and be
     * garbage collected.
     */
    private final DateTime creationDateTime;

    /**
     * an id, identifying the given session instance.
     */
    private final String uuid;

    RenderSessionContextImpl() {
        creationDateTime = new DateTime();
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public DateTime getCreationDateTime() {
        return creationDateTime;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getAgeInSeconds() {
        return new Duration(creationDateTime, new DateTime()).getStandardSeconds();
    }

    @Override
    public void addListVariable(String name) {
        listMap.put(name, new LinkedList<>());
    }

    @Override
    public void addListValue(String listName, Object value) {
        // fun fact: if in HIT/CLOU a variable is accessed which has not been declared before,
        // it gets created at the time of first access.
        if (listMap.get(listName) == null) {
            addListVariable(listName);
        }
        listMap.get(listName).add(value);
    }

    @Override
    public Object setListValueAt(String listName, Integer index, Object value) {
        return listMap.get(listName).set(index, value);
    }

    @Override
    public Object getListValueAt(String listName, int index) {
        checkArgument(!StringUtils.isBlank(listName), "listName is null, empty or consists of whitespace only.");
        listName = listName.trim();
        List<?> list = listMap.get(listName);
        if (list == null) {
            logger.error(StringUtils.join("The given listName ", listName, " has not been initialized. Please use method RenderSessionManager.createList(String renderSessionContextUuid, String listName) to create the list before referencing it."));
            return StringUtils.join("hitassext:ERROR: no list with name ", listName);
        } else if (index < 0 || index >= list.size()) {
            logger.error(StringUtils.join("Index ", index, " invalid for list ", listName, ".size()=", list.size()));
            return StringUtils.join("hitassext:ERROR: IndexOutOfBounds");
        } else {
            return listMap.get(listName).get(index);
        }
    }

    @Override
    public void addScalarVariable(String variableName) {
        scalarMap.put(variableName, "");
    }

    @Override
    public void setScalarVariableValue(String variableName, Object value) {
        scalarMap.put(variableName, value);
    }

    @Override
    public Object getScalarVariableValue(String variableName) {
        Object value = scalarMap.get(variableName);
        if (value == null) {
            logger.error(StringUtils.join("No variable exists for variableName ", variableName));
            return StringUtils.join("hitassext:ERROR: No variable exists for variableName ", variableName);
        } else {
            return value;
        }
    }

    @Override
    public void appendList(String sourceListName, String targetListName) {
        checkArgument(!StringUtils.isBlank(sourceListName), "sourceListName cannot be empty or null!");
        checkArgument(!StringUtils.isBlank(targetListName), "targetListName cannot be empty or null!");
        List sourceList = listMap.get(sourceListName);
        List targetList = listMap.get(targetListName);
        if (sourceList == null) {
            logger.warn(StringUtils.join("No source list for name ", sourceListName,
                    " can be found! No elements will be added to ", targetListName, "."));
            return;
        }
        checkState(targetList != null, StringUtils.join("No target list for name ", targetListName, " can be found!"));
        for (Object item : sourceList) {
            targetList.add(item);
        }
    }

    @Override
    synchronized public int getXmlSequence() {
        return lastQueriedXmlSequence = xmlSequence;
    }

    @Override
    public int getLastQueriedXmlSequence() {
        return lastQueriedXmlSequence;
    }

    @Override
    synchronized public int incrementXmlSequence() {
        return ++xmlSequence;
    }

    @Override
    public int getListLength(String listName) {
        List<?> list = listMap.get(listName);
        if (list == null) {
            logger.error(StringUtils.join("Cannot retrieve length for list ", listName, ", no such list was found."));
            return -1;
        } else {
            return list.size();
        }
    }

    @Override
    public String toString() {
        return "RenderSessionContextImpl{" +
                "creationDateTime=" + creationDateTime +
                ", uuid='" + uuid + '\'' +
                '}';
    }

}
