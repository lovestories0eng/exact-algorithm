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
    private int edgeNum;
    public LinkedList[] adjacencyLinkedList;
    public LinkedList[] adjacencyLinkedListReverse;
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
        edgeNum = 0;
        this.nodeSet = nodeSet;
        this.hashMap = new HashMap<Integer, LinkedList<HeterogeneousEdge>>();
        this.hashMapReverse = new HashMap<Integer, LinkedList<HeterogeneousEdge>>();
        adjacencyLinkedList = new LinkedList[n];
        adjacencyLinkedListReverse = new LinkedList[n];

        for (int i = 0; i < vertexNum; i++) {
            adjacencyLinkedList[i] = new LinkedList<>();
            adjacencyLinkedListReverse[i] = new LinkedList<>();
        }
    }

    /**
     * 插入边
     **/
    public void insertEdge(HeterogeneousEdge edge) {
        int startPoint = edge.getStartPoint();
        int endPoint = edge.getEndPoint();
        // 以起点为基础每个点存储与其相邻的点
        // adjacencyLinkedList[startPoint].add(endPoint);
        adjacencyLinkedList[startPoint].add(edge);
        // 以终点为基础每个点存储与其相邻的点
        // adjacencyLinkedListReverse[endPoint].add(startPoint);
        adjacencyLinkedListReverse[endPoint].add(edge);

        LinkedList<HeterogeneousEdge> tmp = new LinkedList<HeterogeneousEdge>();
        if (hashMap.containsKey(startPoint)) {
            tmp = hashMap.get(startPoint);
            tmp.add(edge);
            hashMap.replace(startPoint, tmp);
        } else {
            tmp.add(edge);
            hashMap.put(startPoint, tmp);

        }

        tmp = new LinkedList<HeterogeneousEdge>();
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
     * 以查询节点为起始节点，初步遍历图，找出有元路径相连的目标节点
     **/
    public void traverseGraph() {

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

        this.hashMap.replace(nodeId, new LinkedList<HeterogeneousEdge>());

        HeterogeneousEdge edge = new HeterogeneousEdge(Constants.SHARED_TIMES);
        edge.setStartPoint(nodeId);
        edge.setEndPoint(virtualNode.id);
        this.insertEdge(edge);
        this.addNode(virtualNode);
        this.vertexNum++;
    }
}
