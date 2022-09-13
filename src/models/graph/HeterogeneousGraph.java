package models.graph;

import global.Config;
import models.Edge.HeterogeneousEdge;
import models.node.HeterogeneousNode;

import java.util.*;

/**
 * 异构图
 **/
public class HeterogeneousGraph {
    public int vertexNum;
    public List<HeterogeneousNode> nodeSet;

    // 用于存储本身的图结构
    public HashMap<Integer, LinkedList<HeterogeneousEdge>> hashMap;
    public HashMap<Integer, LinkedList<HeterogeneousEdge>> hashMapReverse;

    // 用于存储多部图
    public HashMap<Integer, LinkedList<HeterogeneousEdge>> graphHashMap;

    public List<HeterogeneousNode> getNodeSet() {
        return nodeSet;
    }

    public void addNode(HeterogeneousNode node) {
        this.nodeSet.add(node);
    }

    // 生成异构图，以邻接链表方式
    public HeterogeneousGraph(List<HeterogeneousNode> nodeSet) {
        vertexNum = nodeSet.size();
        this.nodeSet = nodeSet;
        this.hashMap = new HashMap<>();
        this.hashMapReverse = new HashMap<>();
    }

    // 重置所有节点的访问状态
    public void reInitNodeset() {
        for (HeterogeneousNode heterogeneousNode : this.nodeSet) {
            heterogeneousNode.visited = false;
        }
    }

    /**
     * 插入边
     **/
    public void insertEdge(HeterogeneousEdge edge) {
        int startPoint = edge.getStartPoint();
        int endPoint = edge.getEndPoint();

        LinkedList<HeterogeneousEdge> tmp = new LinkedList<>();
        if (hashMap.containsKey(startPoint)) {
            tmp = hashMap.get(startPoint);
            tmp.add(edge);
            hashMap.replace(startPoint, tmp);
        } else {
            tmp.add(edge);
            hashMap.put(startPoint, tmp);
        }

        // 为了避免指针相同，创建并复制一个新的对象
        HeterogeneousEdge newEdge = new HeterogeneousEdge(Config.SHARED_TIMES);
        newEdge.startPoint = edge.getStartPoint();
        newEdge.endPoint = edge.getEndPoint();
        tmp = new LinkedList<>();
        if (hashMapReverse.containsKey(endPoint)) {
            tmp = hashMapReverse.get(endPoint);
            tmp.add(newEdge);
            hashMapReverse.replace(endPoint, tmp);
        } else {
            tmp.add(newEdge);
            hashMapReverse.put(endPoint, tmp);
        }
    }

    public void insertMultipartGraph(HeterogeneousEdge edge) {
        int startPoint = edge.getStartPoint();

        LinkedList<HeterogeneousEdge> tmp = new LinkedList<>();
        if (graphHashMap.containsKey(startPoint)) {
            tmp = graphHashMap.get(startPoint);
            tmp.add(edge);
            graphHashMap.replace(startPoint, tmp);
        } else {
            tmp.add(edge);
            graphHashMap.put(startPoint, tmp);
        }



    }

    public void createMultipartGraph(int nodeId) {
        this.graphHashMap = new HashMap<>();

        List<HeterogeneousNode> nodes = new ArrayList<>();

        ArrayList<Boolean> visited = new ArrayList<>();
        for (int i = 0; i < vertexNum; i++) {
            // 全部初始化为false
            visited.add(false);
        }

        // 标记前半段路程的点是否被访问
        ArrayList<Boolean> visitedFormer = new ArrayList<>();
        for (int i = 0; i < vertexNum; i++) {
            visitedFormer.add(false);
        }

        Queue<Integer> queue = new LinkedList<>();
        int currentPathLength = 0;
        int halfMetaPath = Config.META_PATH_LENGTH / 2;

        int nodeIndex = 0;
        for (int i = 0; i < nodeSet.size(); i++) {
            if (nodeSet.get(i).id == nodeId) {
                nodeIndex = i;
            }
        }

        queue.offer(nodeIndex);
        visited.set(nodeIndex, true);
        visitedFormer.set(nodeIndex, true);
        int indexU, u;
        indexU = nodeIndex;
        u = nodeId;
        // 初始化节点类型为空
        String tmpType = "";
        List<HeterogeneousNode> nodeSet = this.nodeSet;
        while (!queue.isEmpty()) {
            indexU = queue.poll();
            u =  nodeSet.get(indexU).id;
            String nodeType = "";
            for (HeterogeneousNode heterogeneousNode : nodeSet) {
                if (heterogeneousNode.id == u) {
                    nodeType = heterogeneousNode.nodeType;
                    nodes.add(heterogeneousNode);
                }
            }
            if (!tmpType.equals(nodeType)) {
                currentPathLength++;
                tmpType = nodeSet.get(indexU).nodeType;
                if (currentPathLength >= Config.META_PATH_LENGTH + 1) {
                    break;
                }
            }
            // 未超过元路径长度的一半时
            if (currentPathLength <= halfMetaPath) {

                int point = hashMap.get(u).get(0).getStartPoint();
                for (int j = 0; j < hashMap.get(point).size(); j++) {
                    HeterogeneousEdge tmpEdge = new HeterogeneousEdge(0);
                    tmpEdge.capacity = hashMap.get(point).get(j).capacity;
                    tmpEdge.startPoint = hashMap.get(point).get(j).startPoint;
                    tmpEdge.endPoint = hashMap.get(point).get(j).endPoint;
                    tmpEdge.direction = true;
                    this.insertMultipartGraph(tmpEdge);
                    // 需要深拷贝
                    // 设置反向路径的容量为0
                    tmpEdge = new HeterogeneousEdge(0);
                    int startPoint = hashMap.get(point).get(j).getStartPoint();
                    int endPoint = hashMap.get(point).get(j).getEndPoint();
                    tmpEdge.startPoint = endPoint;
                    tmpEdge.endPoint = startPoint;
                    tmpEdge.direction = false;
                    this.insertMultipartGraph(tmpEdge);
                }

                for (int i = 0; i < hashMap.get(u).size(); i++) {
                    point = hashMap.get(u).get(i).getEndPoint();
                    int indexPoint = 0;
                    for (int j = 0; j < nodeSet.size(); j++) {
                        if (nodeSet.get(j).id == point) {
                            indexPoint = j;
                        }
                    }
                    if ((Objects.equals(nodeSet.get(indexPoint).nodeType, Config.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(indexU).nodeType, "virtual"))
                            && !visited.get(indexPoint)
                    ) {
                        queue.offer(indexPoint);
                        visited.set(indexPoint, true);
                        visitedFormer.set(indexPoint, true);
                    }
                }
            }
            // 超过元路径长度的一半时
            else {
                // 遍历到目标节点的时候会产生null值
                if (hashMapReverse.get(u) != null) {

                    int point = hashMapReverse.get(u).get(0).getEndPoint();
                    for (int j = 0; j < hashMapReverse.get(point).size(); j++) {
                        // 深拷贝
                        HeterogeneousEdge tmpEdge = new HeterogeneousEdge(0);
                        int startPoint = hashMapReverse.get(point).get(j).getStartPoint();
                        int indexStart = 0;
                        for (int k = 0; k < nodeSet.size(); k++) {
                            if (nodeSet.get(k).id == startPoint) {
                                indexStart = k;
                                break;
                            }
                        }
                        int endPoint = hashMapReverse.get(point).get(j).getEndPoint();
                        if (!visitedFormer.get(indexStart)) {
                            tmpEdge.startPoint = endPoint;
                            tmpEdge.endPoint = startPoint;
                            tmpEdge.capacity = hashMapReverse.get(point).get(j).capacity;
                            tmpEdge.direction = true;
                            this.insertMultipartGraph(tmpEdge);
                            tmpEdge = new HeterogeneousEdge(0);
                            // 设置反向路径的容量为0
                            tmpEdge.startPoint = startPoint;
                            tmpEdge.endPoint = endPoint;
                            tmpEdge.capacity = 0;
                            tmpEdge.direction = false;
                            this.insertMultipartGraph(tmpEdge);
                        }
                    }

                    for (int i = 0; i < hashMapReverse.get(u).size(); i++) {
                        point = hashMapReverse.get(u).get(i).getStartPoint();
                        int indexPoint = 0;
                        for (int j = 0; j < nodeSet.size(); j++) {
                            if (nodeSet.get(j).id == point) {
                                indexPoint = j;
                            }
                        }

                        if ((Objects.equals(nodeSet.get(indexPoint).nodeType, Config.META_PATH[currentPathLength]) || Objects.equals(nodeSet.get(indexU).nodeType, "virtual"))
                                && !visited.get(indexPoint)
                        ) {
                            queue.offer(indexPoint);
                            visited.set(indexPoint, true);
                        }
                    }
                }
            }
        }
        System.out.println("从点" + nodeId + "开始的多部图创建完毕");
    }

    /**
     * 创建与虚拟锚点相连的路径
     * 假设只存在P-A-P类型的元路径，不存在P-A-P-A-P的路径
     **/
    public void createVirtualSinkPath(String nodeType, int virtualSinkNodeId) {
        int nodeNum = nodeSet.size();
        // 创建出一个虚拟锚点
        HeterogeneousNode virtualSinkNode = new HeterogeneousNode();
        virtualSinkNode.id = virtualSinkNodeId;
        virtualSinkNode.nodeType = "sink";

        List<HeterogeneousNode> nodeSet = this.nodeSet;

        // 遍历图中所有的点，找出所有目标类型节点并且使其与虚拟锚点相连，从而可以使用网络最大流算法
        for (int i = 0; i < nodeNum; i++) {
            if (Objects.equals(nodeSet.get(i).nodeType, nodeType)
            ) {
                HeterogeneousEdge edge = new HeterogeneousEdge(1);
                edge.setStartPoint(nodeSet.get(i).id);
                edge.setEndPoint(virtualSinkNode.id);
                edge.direction = true;
                this.insertMultipartGraph(edge);

                edge = new HeterogeneousEdge(0);
                edge.setStartPoint(virtualSinkNode.id);
                edge.setEndPoint(nodeSet.get(i).id);
                edge.direction = false;
                this.insertMultipartGraph(edge);
            }
        }
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
            HeterogeneousEdge edge = new HeterogeneousEdge(Config.SHARED_TIMES);
            edge.setStartPoint(virtualNode.id);
            int endPoint = this.hashMap.get(nodeId).get(i).getEndPoint();
            edge.setEndPoint(endPoint);
            this.insertEdge(edge);
        }

        this.hashMap.replace(nodeId, new LinkedList<>());

        HeterogeneousEdge edge = new HeterogeneousEdge(Config.SHARED_TIMES);
        edge.setStartPoint(nodeId);
        edge.setEndPoint(virtualNode.id);
        this.insertEdge(edge);
        this.addNode(virtualNode);
        this.vertexNum++;
    }
}
