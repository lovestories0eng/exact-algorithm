package models.graph;

import models.Edge.HomogeneousEdge;
import models.node.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * 同构图
 * **/
public class HomogeneousGraph {
    public ArrayList<Node> nodeSet;
    // 邻接链表存储图结构
    public HashMap<Integer, LinkedList<HomogeneousEdge>> hashMap;

    public HomogeneousGraph() {
        this.hashMap = new HashMap<>();
        this.nodeSet = new ArrayList<>();
    }

    public void addNodes(ArrayList<Node> nodes) {
        this.nodeSet.addAll(nodes);
    }

    public void insertEdge(HomogeneousEdge edge) {
        int pointFirst = edge.pointFirst;
        int pointSecond = edge.pointSecond;
        LinkedList<HomogeneousEdge> tmp;
        if (hashMap.containsKey(pointFirst)) {
            tmp = hashMap.get(pointFirst);
            tmp.add(edge);
            hashMap.replace(pointFirst, tmp);
        } else {
            tmp = new LinkedList<>();
            tmp.add(edge);
            hashMap.put(pointFirst, tmp);
        }

        if (hashMap.containsKey(pointSecond)) {
            tmp = hashMap.get(pointSecond);
            tmp.add(edge);
            hashMap.replace(pointSecond, tmp);
        } else {
            tmp = new LinkedList<>();
            tmp.add(edge);
            hashMap.put(pointSecond, tmp);
        }
    }
}
