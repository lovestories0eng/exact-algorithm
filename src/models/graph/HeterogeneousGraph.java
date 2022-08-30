package models.graph;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.node.HeterogeneousNode;

import java.util.*;

/**
 * 异构图
 **/
public class HeterogeneousGraph {
    public int vertexNum;
    // public LinkedList[] adjacencyLinkedList;
    // public LinkedList[] adjacencyLinkedListReverse;
    private List<HeterogeneousNode> nodeSet;

    public HashMap<Integer, LinkedList<HeterogeneousEdge>> hashMap;
    public HashMap<Integer, LinkedList<HeterogeneousEdge>> hashMapReverse;

    public List<HeterogeneousNode> getNodeSet() {
        return nodeSet;
    }

    public void addNode(HeterogeneousNode node) {
        this.nodeSet.add(node);
    }

    // 生成异构图，以邻接链表方式
    public HeterogeneousGraph(int n, List<HeterogeneousNode> nodeSet) {
        vertexNum = nodeSet.size();
        this.nodeSet = nodeSet;
        this.hashMap = new HashMap<>();
        this.hashMapReverse = new HashMap<>();
        // adjacencyLinkedList = new LinkedList[n];
        // adjacencyLinkedListReverse = new LinkedList[n];

        // for (int i = 0; i < vertexNum; i++) {
            // adjacencyLinkedList[i] = new LinkedList<>();
            // adjacencyLinkedListReverse[i] = new LinkedList<>();
        // }
    }

    /**
     * 插入边
     **/
    public void insertEdge(HeterogeneousEdge edge) {
        int startPoint = edge.getStartPoint();
        int endPoint = edge.getEndPoint();
        // 以起点为基础每个点存储与其相邻的点
        // adjacencyLinkedList[startPoint].add(endPoint);
        // adjacencyLinkedList[startPoint].add(edge);
        // 以终点为基础每个点存储与其相邻的点
        // adjacencyLinkedListReverse[endPoint].add(startPoint);
        // adjacencyLinkedListReverse[endPoint].add(edge);

        LinkedList<HeterogeneousEdge> tmp = new LinkedList<>();
        if (hashMap.containsKey(startPoint)) {
            tmp = hashMap.get(startPoint);
            tmp.add(edge);
            hashMap.replace(startPoint, tmp);
        } else {
            tmp.add(edge);
            hashMap.put(startPoint, tmp);

        }

        tmp = new LinkedList<>();
        if (hashMapReverse.containsKey(endPoint)) {
            tmp = hashMapReverse.get(endPoint);
            tmp.add(edge);
            hashMapReverse.replace(endPoint, tmp);
        } else {
            tmp.add(edge);
            hashMapReverse.put(endPoint, tmp);
        }

    }

    /**
     * 创建虚拟锚点
     * 假设只存在P-A-P类型的元路径，不存在P-A-P-A-P的路径
     **/
    public void createVirtualNode(int nodeId) {
        int nodeNum = nodeSet.size();
        // 创建出一个虚拟锚点
        HeterogeneousNode virtualNode = new HeterogeneousNode();
        virtualNode.id = nodeNum - 1;

        List<HeterogeneousNode> nodeSet = this.nodeSet;

        // 遍历图中所有的点，找出所有目标类型节点并且使其与虚拟锚点相连，从而可以使用网络最大流算法
        for (int i = 0; i < nodeNum; i++) {
            if (i != nodeId && Objects.equals(nodeSet.get(i).nodeType, nodeSet.get(nodeId).nodeType)) {
                HeterogeneousEdge edge = new HeterogeneousEdge(Constants.SHARED_TIMES);
                edge.setStartPoint(i);
                edge.setEndPoint(virtualNode.id);
                this.insertEdge(edge);
            }
        }
        this.addNode(virtualNode);
        this.vertexNum++;
    }

    /**
     * 在bfsTraverse初步遍历后生成导出子图
     **/
    public void createInducedGraph(List<HeterogeneousNode> nodeSet) {
        this.hashMap = this.deleteLinkEdge(hashMap, nodeSet);
        this.hashMapReverse = this.deleteLinkEdge(hashMapReverse, nodeSet);
        this.nodeSet = nodeSet;
        this.vertexNum = this.nodeSet.size();
        System.out.println("初步筛选后的图尺寸");
        System.out.println(this.nodeSet.size());
    }

    public HashMap<Integer, LinkedList<HeterogeneousEdge>> deleteLinkEdge(HashMap<Integer, LinkedList<HeterogeneousEdge>> map, List<HeterogeneousNode> nodeSet) {
        Map.Entry<Integer, LinkedList<HeterogeneousEdge>> entry;
        Iterator<Map.Entry<Integer, LinkedList<HeterogeneousEdge>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            entry = iterator.next();
            // 不存在的点则直接删除key
            if (judgeExist(entry.getKey(), nodeSet)) {
                iterator.remove();
            } else {
                map.replace(entry.getKey(), deleteUnExistEdge(entry.getValue(), nodeSet));
            }
        }
        return map;
    }

    public boolean judgeExist(int nodeId, List<HeterogeneousNode> nodeSet) {
        for (HeterogeneousNode heterogeneousNode : nodeSet) {
            if (heterogeneousNode.id == nodeId) {
                return false;
            }
        }
        return true;
    }

    public LinkedList<HeterogeneousEdge> deleteUnExistEdge(
            LinkedList<HeterogeneousEdge> heterogeneousEdges,
            List<HeterogeneousNode> nodeSet
    ) {
        LinkedList<HeterogeneousEdge> newEdges = new LinkedList<>();
        for (HeterogeneousEdge edge : heterogeneousEdges) {
            if (!(judgeExist(edge.getStartPoint(), nodeSet) ||
                    judgeExist(edge.getEndPoint(), nodeSet))
            ) {
                newEdges.add(edge);
            }
        }
        return newEdges;
    }

    /**
     * 分裂一个节点，使其变成两个虚拟节点（由边不相交转换成点不相交）
     **/
    public void nodeSplit(int nodeId) {
        HeterogeneousNode virtualNode = new HeterogeneousNode();
        virtualNode.id = this.nodeSet.size();
        // 派生出来的节点默认类型设置为"virtual"便于处理
        virtualNode.nodeType = "virtual";

        for (int i = 0; i < this.hashMap.get(nodeId).size(); i++) {
            HeterogeneousEdge edge = new HeterogeneousEdge(Constants.SHARED_TIMES);
            edge.setStartPoint(virtualNode.id);
            int endPoint = this.hashMap.get(nodeId).get(i).getEndPoint();
            edge.setEndPoint(endPoint);
            this.insertEdge(edge);
        }

        this.hashMap.replace(nodeId, new LinkedList<>());

        HeterogeneousEdge edge = new HeterogeneousEdge(Constants.SHARED_TIMES);
        edge.setStartPoint(nodeId);
        edge.setEndPoint(virtualNode.id);
        this.insertEdge(edge);
        this.addNode(virtualNode);
        this.vertexNum++;
    }
}
