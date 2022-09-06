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
        boolean flag = false;

        for (Node node : nodes) {
            for (Node point : nodeSet) {
                if (point.id == node.id) {
                    flag = true;
                    break;
                }
            }
            if (!flag)
                this.nodeSet.add(node);
        }
    }

    public void insertEdge(HomogeneousEdge edge) {
        int pointFirst = edge.pointFirst;
        int pointSecond = edge.pointSecond;

        // 如果边已存在，则直接返回函数
        if (hashMap.get(pointFirst) != null) {
            for (int i = 0; i < hashMap.get(pointFirst).size(); i++) {
                if ((hashMap.get(pointFirst).get(i).pointFirst == pointFirst && hashMap.get(pointFirst).get(i).pointSecond == pointSecond)
                    ||(hashMap.get(pointFirst).get(i).pointSecond == pointFirst && hashMap.get(pointFirst).get(i).pointFirst == pointSecond))
                    return;
            }
        }

        if (hashMap.get(pointSecond) != null) {
            for (int i = 0; i < hashMap.get(pointSecond).size(); i++) {
                if ((hashMap.get(pointSecond).get(i).pointFirst == pointFirst && hashMap.get(pointSecond).get(i).pointSecond == pointSecond)
                        ||(hashMap.get(pointSecond).get(i).pointSecond == pointFirst && hashMap.get(pointSecond).get(i).pointFirst == pointSecond))
                    return;
            }
        }

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

        HomogeneousEdge newEdge = new HomogeneousEdge();
        newEdge.pointFirst = pointSecond;
        newEdge.pointSecond = pointFirst;

        if (hashMap.containsKey(pointSecond)) {
            tmp = hashMap.get(pointSecond);
            tmp.add(newEdge);
            hashMap.replace(pointSecond, tmp);
        } else {
            tmp = new LinkedList<>();
            tmp.add(newEdge);
            hashMap.put(pointSecond, tmp);
        }
    }
}
