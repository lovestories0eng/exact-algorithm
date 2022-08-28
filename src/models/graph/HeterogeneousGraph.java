package models.graph;

import global.Constants;
import models.Edge.HeterogeneousEdge;
import models.node.HeterogeneousNode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 异构图
 **/
public class HeterogeneousGraph {
    public int vertexNum;
    private int edgeNum;
    public LinkedList[] adjacencyLinkedList;
    public LinkedList[] adjacencyLinkedListReverse;
    private List<HeterogeneousNode> nodeSet;

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
        adjacencyLinkedList = new LinkedList[n];
        adjacencyLinkedListReverse = new LinkedList[n];
        this.nodeSet = nodeSet;

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
        adjacencyLinkedList[startPoint].add(endPoint);
        // 以终点为基础每个点存储与其相邻的点
        adjacencyLinkedListReverse[endPoint].add(startPoint);
    }

    /**
     * 删除边
     **/
    public void deleteEdge(HeterogeneousEdge edge) {
        int startPoint = edge.getStartPoint();
        int endPoint = edge.getEndPoint();
        adjacencyLinkedList[startPoint].remove((Integer) endPoint);
        adjacencyLinkedListReverse[endPoint].remove((Integer) startPoint);
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
    }

    /**
     * 分裂一个节点，使其变成两个虚拟节点（由边不相交转换成点不相交）
     **/
    public void nodeSplit(int nodeId) {
        HeterogeneousNode virtualNode = new HeterogeneousNode();
        virtualNode.id = this.nodeSet.size();
        virtualNode.nodeType = "virtual";

        LinkedList[] newAdjacencyLinkedList = new LinkedList[this.adjacencyLinkedList.length + 1];
        System.arraycopy(this.adjacencyLinkedList, 0, newAdjacencyLinkedList, 0, this.adjacencyLinkedList.length);
        this.adjacencyLinkedList = newAdjacencyLinkedList;

        LinkedList[] newAdjacencyLinkedListReverse = new LinkedList[this.adjacencyLinkedListReverse.length + 1];
        System.arraycopy(this.adjacencyLinkedListReverse, 0, newAdjacencyLinkedListReverse, 0, this.adjacencyLinkedListReverse.length);
        this.adjacencyLinkedListReverse = newAdjacencyLinkedListReverse;

        for (int i = 0; i < adjacencyLinkedList[nodeId].size(); i++) {
            HeterogeneousEdge edge = new HeterogeneousEdge(Constants.SHARED_TIMES);
            edge.setStartPoint(virtualNode.id);
            int endPoint = (int) adjacencyLinkedList[nodeId].get(i);
            edge.setEndPoint(endPoint);
            this.insertEdge(edge);
        }
        this.adjacencyLinkedList[nodeId] = new LinkedList();
        this.adjacencyLinkedListReverse[virtualNode.id].add(nodeId);
        HeterogeneousEdge edgeLink = new HeterogeneousEdge(Constants.SHARED_TIMES);
        edgeLink.setStartPoint(nodeId);
        edgeLink.setEndPoint(virtualNode.id);
        this.insertEdge(edgeLink);
        this.addNode(virtualNode);
    }
}
