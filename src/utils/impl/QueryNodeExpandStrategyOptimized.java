package utils.impl;

import common.BatchLinker;
import global.Config;
import models.MetaPath;
import utils.InitialGraphConstructor;

import java.util.*;

// TODO: 删除vertexPair的同时更新阶梯索引
// 从查询点扩展构建初始图
public class QueryNodeExpandStrategyOptimized implements InitialGraphConstructor {
    private final int[][] graph;//data graph, including vertex IDs, edge IDs, and their link relationships
    private final int[] vertexType;//vertex -> type
    private final int[] edgeType;//edge -> type
    private final int[] edgeUsedTimes;//edge -> used times
    private final int[] vertexUsedTimes;
    private final HashMap<Integer, Set<Integer>> homoGraph = new HashMap<>();
    int hops;
    // 记录找到的所有节点
    Set<Integer> vertexFound = new HashSet<>();
    HashMap<Integer, ArrayList<Integer>> pathRecordMap = new HashMap<>();
    Set<Integer> keepSet = null;
    HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge;
    // path conflict
    HashMap<ArrayList<Integer>, Double> pathMapConflict;
    // <vertex pair -> path with lowest conflict>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<Integer>> vertexPairMapPath;
    // <vertex pair -> path conflict>
    HashMap<Map.Entry<Integer, Integer>, Double> vertexPairMapConflict;
    // <vertex pair -> path conflict ordered>
    // TODO: 点对的冲突度是呈阶梯的，在排序好之后记录每一个冲突度的阶梯
    // ArrayList<Map.Entry<Integer, Integer>> vertexPairMapConflictOrdered;
    // <vertex pair -> all path between two vertex>
    HashMap<Map.Entry<Integer, Integer>, ArrayList<ArrayList<Integer>>> vertexPairMapAllPath;
    // 存储所有路径
    ArrayList<ArrayList<Integer>> allPaths = new ArrayList<>();
    // <edge id -> ArrayList<path>>
    HashMap<Integer, ArrayList<ArrayList<Integer>>> edgeMapPath;
    // 存储通过一跳搜索找到的新的节点
    Set<Integer> newFoundVertex = new HashSet<>();
    // 存储上一次一跳搜索找到的点
    Set<Integer> foundVertex = new HashSet<>();
    int pathLen = 0;
    double globalVertexPairConflict;
    double globalPathConflict;
    // 存储冲突度最小的点对集合
    HashMap<Double, ArrayList<ArrayList<Integer>>> conflictMapVertexPairPath;
    // 存储阶梯冲突度的索引
    HashMap<Double, Integer> stepConflictIndex = new HashMap<>();
    // 存储vertexPair的大小
    ArrayList<Integer> vertexPairRecorder = new ArrayList<>();

    public QueryNodeExpandStrategyOptimized(int[][] graph, int[] vertexType, int[] edgeType, int[] edgeUsedTimes, int[] vertexUsedTimes, HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge, int hops) {
        this.graph = graph;
        this.vertexType = vertexType;
        this.edgeType = edgeType;
        this.edgeUsedTimes = edgeUsedTimes;
        this.vertexUsedTimes = vertexUsedTimes;
        this.vertexPairMapEdge = vertexPairMapEdge;
        this.hops = hops;
    }

    public int[][] query(int queryId, MetaPath metaPath, int mode) {

        for (int i = 1; i <= Config.SHARED_TIMES * metaPath.pathLen; i++) {
            // 负一代表没有此种冲突度
            stepConflictIndex.put((double) (i / (Config.SHARED_TIMES * metaPath.pathLen)), -1);
        }

        this.pathLen = metaPath.pathLen + 1;
        if (metaPath.vertex[0] != vertexType[queryId]) {
            System.out.println("查询节点类型与元路径类型不匹配！");
            return null;
        }

        // 存储寻找到的所有节点
        vertexFound.add(queryId);
        foundVertex.add(queryId);

        // step 1: compute the connected subgraph via batch-search with labeling (BSL)
        BatchLinker batchLinker = new BatchLinker(graph, vertexType, edgeType);
        keepSet = batchLinker.link(queryId, metaPath);

        //step 2: perform pruning
        // 根据路径的第一条边删除了一遍出度<(k / a)的节点
        // SECONDVertexTYPE: P, SECONDEdgeTYPE: A -> P
        if (mode == 1) {
            int SECONDVertexTYPE = metaPath.vertex[1], SECONDEdgeTYPE = metaPath.edge[0];
            Iterator<Integer> keepIter = keepSet.iterator();
            while (keepIter.hasNext()) {
                int id = keepIter.next();
                int count = 0;
                // 遍历所有与查询节点p邻居的点
                for (int i = 0; i < graph[id].length; i += 2) {
                    int nbVId = graph[id][i], nbEId = graph[id][i + 1];
                    if (vertexType[nbVId] == SECONDVertexTYPE && edgeType[nbEId] == SECONDEdgeTYPE) {
                        count++;
                        if (count >= ((Config.k - 1) / Config.SHARED_TIMES)) break;
                    }
                }
                if (count < ((Config.k - 1) / Config.SHARED_TIMES)) keepIter.remove();
            }
        }

        if (!keepSet.contains(queryId)) return null;

        this.createHomoGraph(metaPath);
        this.homoGraphAnalyzer(metaPath);

        TrussDecomposition trussDecomposition = new TrussDecomposition(homoGraph);

        HashMap<Integer, Integer> maxK;

        Set<Integer> result;

        if (mode == 0) {
            maxK = trussDecomposition.findMaxKTruss();
            for (Map.Entry<Integer, Integer> entry : maxK.entrySet()) {
                System.out.printf("%d, %d\n", entry.getKey(), entry.getValue());
            }
        } else if (mode == 1) {
            result = trussDecomposition.executeDecompose();
            System.out.println(result.size());
        }
        return null;
    }


    private void homoGraphAnalyzer(MetaPath metaPath) {
        double totalUsedTimes = 0;
        for (Map.Entry<Integer, Set<Integer>> integerSetEntry : homoGraph.entrySet()) {
            Set<Integer> adjacent = integerSetEntry.getValue();
            totalUsedTimes += adjacent.size() * metaPath.pathLen;
        }
        totalUsedTimes /= 2;
        double usedPercent = totalUsedTimes / (Config.SHARED_TIMES * edgeUsedTimes.length);
        System.out.println("边总使用次数：" + totalUsedTimes);
        System.out.println("共享次数使用百分比：" + usedPercent);

        System.out.println("每次迭代中新的点对数");
        for (Integer integer : vertexPairRecorder) {
            System.out.println(integer);
        }
        int vertexInTotal = 0;
        int edgeInTotal = 0;
        Map.Entry<Integer, Set<Integer>> entry;
        for (Map.Entry<Integer, Set<Integer>> integerSetEntry : homoGraph.entrySet()) {
            entry = integerSetEntry;
            vertexInTotal++;
            edgeInTotal += entry.getValue().size();
        }
        edgeInTotal /= 2;
        System.out.println("同构图点数：" + vertexInTotal);
        System.out.println("同构图边数：" + edgeInTotal);
    }

    private void createHomoGraph(MetaPath metaPath) {
        long startTime = System.currentTimeMillis();
        int curHop = 0;
        HashMap<Integer, Integer> vertexMapDegrees;

        while (curHop < this.hops) {
            if (curHop != 0) {
                // step 3: link edges inner the new-found vertex
                newFoundVertex = this.oneHopTraverse(foundVertex, metaPath, false);
                if (newFoundVertex.size() != 0) {
                    // 根据树结构得到所有路径
                    createPathSet(foundVertex, pathRecordMap);
                    // step 4: link edges between new-found vertex set and original vertex set according to the pair conflict rule
                    initPathMapConflict();
                    // link edges with the lowest conflict utils vertex pairs are linked or no edges exists
                    foundVertex = new HashSet<>();
                    traverseVertexPair();
                }
            }
            // step 5: expand the graph from the new-found node, find possibly linked vertex

            vertexMapDegrees = new HashMap<>();

            newFoundVertex = oneHopTraverse(foundVertex, metaPath, true);
            // 如果找不到新的点则结束
            if (newFoundVertex.size() == 0) break;
            createPathSet(foundVertex, pathRecordMap);

            // 记录各个点的相关度数
            for (ArrayList<Integer> allPath : allPaths) {
                int vertexId = allPath.get(0);
                if (!vertexMapDegrees.containsKey(vertexId)) {
                    vertexMapDegrees.put(vertexId, 1);
                } else {
                    int oldDegree = vertexMapDegrees.get(vertexId);
                    vertexMapDegrees.put(vertexId, oldDegree + 1);
                }
            }

            // 加上原来同构图中的度数
            for (Map.Entry<Integer, Integer> entry : vertexMapDegrees.entrySet()) {
                int vertexId = entry.getKey();
                if (homoGraph.containsKey(vertexId)) {
                    int oldDegree = vertexMapDegrees.get(vertexId);
                    vertexMapDegrees.put(vertexId, oldDegree + homoGraph.get(vertexId).size());
                }
            }

            System.out.println("Break point!");
            initPathMapConflict();
            // initPathMapConflict(vertexMapDegrees);
            traverseVertexPair();
            curHop++;

            // 如果已经到了最后一轮则内扩并且结束循环
            if (curHop == this.hops) {
                // step 3: link edges inner the new-found vertex
                newFoundVertex = this.oneHopTraverse(foundVertex, metaPath, false);
                if (newFoundVertex.size() != 0) {
                    // 根据树结构得到所有路径
                    createPathSet(foundVertex, pathRecordMap);
                    // step 4: link edges between new-found vertex set and original vertex set according to the pair conflict rule
                    initPathMapConflict();
                    // link edges with the lowest conflict utils vertex pairs are linked or no edges exists
                    foundVertex = new HashSet<>();
                    traverseVertexPair();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("运行时间：" + (endTime - startTime) + "ms");
    }

    private void initPathMapConflict() {
        pathMapConflict = new HashMap<>();
        for (ArrayList<Integer> tmpPath : allPaths) {
            double conflict = calcPathConflict(tmpPath);
            if (conflict != -1) {
                pathMapConflict.put(tmpPath, conflict);
            }
        }
        this.initVertexPairMap();
    }

    // 函数重载
    private void initPathMapConflict(HashMap<Integer, Integer> vertexMapDegrees) {
        pathMapConflict = new HashMap<>();
        for (ArrayList<Integer> tmpPath : allPaths) {
            int vertexId = tmpPath.get(0);
            if (vertexMapDegrees.get(vertexId) >= Config.k - 1) {
                double conflict = calcPathConflict(tmpPath);
                if (conflict != -1) {
                    pathMapConflict.put(tmpPath, conflict);
                }
            }
        }
        this.initVertexPairMap();
    }

    private void traverseVertexPair() {
        // 监控每一次找到的点的数量
        vertexPairRecorder.add(vertexPairMapConflict.size());
        while (vertexPairMapConflict.size() != 0) {
            double minConflict = Double.POSITIVE_INFINITY;
            // TODO: 把点对按冲突度进行分组，实现O(1)时间内找到冲突度最小的点对
            // TODO: 如果vertexPair已连边则跳过
            Map.Entry<Integer, Integer> tmp = null;
            int count = 0;
            for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
                if (entry.getValue() < minConflict) {
                    tmp = entry.getKey();
                    minConflict = entry.getValue();
                }
                count++;
                if (count == vertexPairMapConflict.size() && !(minConflict - globalVertexPairConflict < 1e-4)) {
                    globalVertexPairConflict = minConflict;
                }
                // 如果已经找到了全局最小冲突值则跳出循环
                if (minConflict == globalVertexPairConflict) break;
            }
            ArrayList<Integer> path = vertexPairMapPath.get(tmp);

            // 把发现的点加入”已找到点“集合中
            vertexFound.add(path.get(path.size() - 1));
            foundVertex.add(path.get(path.size() - 1));
            homeGraphBuilder(path);
            // update path conflict and vertex pair conflict
            updateVertexPairMap(path);
        }
    }

    private void initVertexPairMap() {
        vertexPairMapConflict = new HashMap<>();
        vertexPairMapPath = new HashMap<>();
        globalVertexPairConflict = Double.POSITIVE_INFINITY;

        vertexPairMapAllPath = new HashMap<>();
        globalPathConflict = Double.POSITIVE_INFINITY;

        for (Map.Entry<ArrayList<Integer>, Double> entry : pathMapConflict.entrySet()) {
            ArrayList<Integer> key = entry.getKey();
            double conflict = entry.getValue();
            if (conflict < globalPathConflict) {
                globalPathConflict = conflict;
            }
            int k = key.get(0);
            int v = key.get(key.size() - 1);
            // 判断边容量是否大于0
            boolean flag = true;
            for (int i = 0; i < key.size() - 1; i++) {
                int tmpK = key.get(i), tmpV = key.get(i + 1);
                if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(tmpK, tmpV))] <= 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // 记录点对间的所有路径
                if (vertexPairMapAllPath.containsKey(Map.entry(k, v))) {
                    ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(Map.entry(k, v));
                    tmp.add(key);
                } else {
                    ArrayList<ArrayList<Integer>> tmp = new ArrayList<>();
                    tmp.add(key);
                    vertexPairMapAllPath.put(Map.entry(k, v), tmp);
                }
                // 记录点对间的最小冲突值
                if (vertexPairMapConflict.containsKey(Map.entry(k, v))) {
                    if (conflict < vertexPairMapConflict.get(Map.entry(k, v))) {
                        vertexPairMapConflict.put(Map.entry(k, v), conflict);
                        vertexPairMapPath.put(Map.entry(k, v), key);
                    }
                } else {
                    vertexPairMapConflict.put(Map.entry(k, v), conflict);
                    vertexPairMapPath.put(Map.entry(k, v), key);
                }
                if (conflict < globalVertexPairConflict) {
                    globalVertexPairConflict = conflict;
                }
            }
        }

        // vertexPairMapConflictOrdered = new ArrayList<>();

        // 初始化点对冲突度数组
        // for (Map.Entry<Map.Entry<Integer, Integer>, Double> entry : vertexPairMapConflict.entrySet()) {
        // vertexPairMapConflictOrdered.add(entry.getKey());
        // }

        // this.mergeSort();

        // double tmpConflict = -1;

        // for (int i = 0; i < vertexPairMapConflictOrdered.size(); i++) {
        //     double conflict = vertexPairMapConflict.get(vertexPairMapConflictOrdered.get(i));
        //     if (conflict != tmpConflict) {
        //         stepConflictIndex.put(conflict, i);
        //         tmpConflict = conflict;
        //     }
        // }
        // System.out.println("Break point!");
    }

    // public void mergeSort() {
    //     Map.Entry<Integer, Integer>[] temp = new Map.Entry[vertexPairMapConflictOrdered.size()];
    //     int gap = 1;
    //
    //     while (gap < vertexPairMapConflictOrdered.size()) {
    //         for (int i = 0; i < vertexPairMapConflictOrdered.size(); i += gap * 2) {
    //             int mid = i + gap;
    //             int right = mid + gap;
    //
    //             if (mid > vertexPairMapConflictOrdered.size()) {
    //                 mid = vertexPairMapConflictOrdered.size();
    //             }
    //             if (right > vertexPairMapConflictOrdered.size()) {
    //                 right = vertexPairMapConflictOrdered.size();
    //             }
    //             mergeData(i, mid, right, temp);
    //         }
    //         for (int i = 0; i < vertexPairMapConflictOrdered.size(); i++) {
    //             vertexPairMapConflictOrdered.set(i, temp[i]);
    //         }
    //         // gap *= 2;
    //         gap <<= 1;
    //     }
    // }

    // // 合并数据  [left,mid)  [mid,right)
    // private void mergeData(int left, int mid, int right, Map.Entry[] temp) {
    //     int index = left;
    //     int begin1 = left, begin2 = mid;
    //
    //     while (begin1 < mid && begin2 < right) {
    //         if (vertexPairMapConflict.get(vertexPairMapConflictOrdered.get(begin1))
    //                 <= vertexPairMapConflict.get(vertexPairMapConflictOrdered.get(begin2))) {
    //             temp[index++] = vertexPairMapConflictOrdered.get(begin1++);
    //         } else {
    //             temp[index++] = vertexPairMapConflictOrdered.get(begin2++);
    //         }
    //     }
    //     // 如果第一个区间中还有数据
    //     while (begin1 < mid) {
    //         temp[index++] = vertexPairMapConflictOrdered.get(begin1++);
    //     }
    //     // 如果第二个区间有数据
    //     while (begin2 < right) {
    //         temp[index++] = vertexPairMapConflictOrdered.get(begin2++);
    //     }
    // }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void homeGraphBuilder(ArrayList<Integer> path) {
        Set<Integer> set = homoGraph.get(path.get(0));
        if (set != null) {
            set.add(path.get(path.size() - 1));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(path.size() - 1));
            homoGraph.put(path.get(0), newSet);
        }
        set = homoGraph.get(path.get(path.size() - 1));
        if (set != null) {
            set.add(path.get(0));
        } else {
            Set<Integer> newSet = new HashSet<>();
            newSet.add(path.get(0));
            homoGraph.put(path.get(path.size() - 1), newSet);
        }
        // 更新边容量
        for (int i = 0; i < path.size() - 1; i++) {
            edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))]--;
        }
        // 如果点对已连接则去除其在HashMap中的键值对
        int k = path.get(0);
        int v = path.get(path.size() - 1);

        // Double tmpConflict1 = vertexPairMapConflict.get(Map.entry(k, v));
        // Double tmpConflict2 = vertexPairMapConflict.get(Map.entry(v, k));
        vertexPairMapConflict.remove(Map.entry(k, v));
        vertexPairMapPath.remove(Map.entry(k, v));
        vertexPairMapAllPath.remove(Map.entry(k, v));
        vertexPairMapConflict.remove(Map.entry(v, k));
        vertexPairMapPath.remove(Map.entry(v, k));
        vertexPairMapAllPath.remove(Map.entry(v, k));
        // vertexPairMapConflictOrdered.remove(Map.entry(k, v));
        // vertexPairMapConflictOrdered.remove(Map.entry(v, k));

    }

    /**
     * flag: 0 -> vertex pair, 1 -> path
     **/
    private void updateVertexPairMap(ArrayList<Integer> path) {
        for (int i = 0; i < path.size() - 1; i++) {
            int k = path.get(i);
            int v = path.get(i + 1);
            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
            ArrayList<ArrayList<Integer>> tmpVar = edgeMapPath.get(edgeId);
            for (int j = 0; j < tmpVar.size(); j++) {
                // TODO: 如果此条路径所连接的点对已经无了则无需更新路径，并且可以删掉路径
                ArrayList<Integer> integers = tmpVar.get(j);
                // 更新边冲突
                double conflict = calcPathConflict(integers);
                int vertexPairStart = integers.get(0);
                int vertexPairEnd = integers.get(integers.size() - 1);
                Map.Entry<Integer, Integer> vertexPair = Map.entry(vertexPairStart, vertexPairEnd);
                // Map.Entry<Integer, Integer> vertexPairReverse = Map.entry(vertexPairEnd, vertexPairStart);
                // 如果点对在同构图中已连接则跳过
                if (!vertexPairMapConflict.containsKey(vertexPair))
                    continue;
                if (pathMapConflict.containsKey(integers)) {
                    pathMapConflict.put(integers, conflict);
                }
                // 更新点对冲突
                // double oldConflict = vertexPairMapConflict.get(vertexPair);
                double newConflict = conflict;
                if (newConflict == -1) {
                    ArrayList<ArrayList<Integer>> tmp = vertexPairMapAllPath.get(vertexPair);
                    tmp.remove(integers);
                    tmpVar.remove(integers);
                    // 防止索引越界
                    j--;
                    // 如果此点对之间已无路径了则删除点对的键值，直接进行下一轮循环
                    if (tmp.size() == 0) {
                        vertexPairMapConflict.remove(vertexPair);
                        vertexPairMapPath.remove(vertexPair);
                        vertexPairMapAllPath.remove(vertexPair);
                        // vertexPairMapConflictOrdered.remove(vertexPair);
                        // vertexPairMapConflict.remove(vertexPairReverse);
                        // vertexPairMapPath.remove(vertexPairReverse);
                        // vertexPairMapAllPath.remove(vertexPairReverse);
                        continue;
                    }
                    // 如果恰巧这条路径就是当前vertexMapPath中的路径
                    if (integers == vertexPairMapPath.get(vertexPair)) {
                        Double tmpConflict = Double.POSITIVE_INFINITY;
                        // 另外找出一条冲突最小的路径
                        for (ArrayList<Integer> minPath : tmp) {
                            Double anotherConflict = pathMapConflict.get(minPath);
                            if (anotherConflict < tmpConflict) {
                                vertexPairMapConflict.put(vertexPair, anotherConflict);
                                vertexPairMapPath.put(vertexPair, minPath);
                            }
                        }
                    }
                }
                // A->P->A路径不会出现这种情况，因为是冲突度是阶梯性的
                // else if (newConflict < oldConflict) {
                //     System.out.println("replaced");
                //     vertexPairMapConflict.put(vertexPair, newConflict);
                //     vertexPairMapPath.put(vertexPair, integers);
                // }
            }
        }
    }

    private double calcPathConflict(ArrayList<Integer> path) {
        double totalTimes = Config.SHARED_TIMES;
        double usedTimesInTotal = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            usedTimesInTotal += totalTimes - edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))];
            // 如果有边的容量小于等于0
            if (edgeUsedTimes[vertexPairMapEdge.get(Map.entry(path.get(i), path.get(i + 1)))] <= 0) {
                // -1代表此条路径已经不可用
                return -1;
            }
        }
        return usedTimesInTotal / (totalTimes * (path.size() - 1));
    }

    /**
     * flag: to indicate whether we find edges from unknown vertex or known vertex
     **/
    private Set<Integer> oneHopTraverse(Set<Integer> startSet, MetaPath metaPath, boolean flag) {
        // TODO: Optimization need, try to filter some vertex
        int pathLen = metaPath.pathLen;
        Set<Integer> batchSet = startSet;
        pathRecordMap = new HashMap<>();
        allPaths = new ArrayList<>();
        for (int index = 0; index < pathLen; index++) {
            int startPoint = 0;
            int targetVType = metaPath.vertex[index + 1], targetEType = metaPath.edge[index];
            Set<Integer> nextBatchSet = new HashSet<>();
            for (int anchorId : batchSet) {
                int[] nbArr = graph[anchorId];
                for (int i = 0; i < nbArr.length; i += 2) {
                    int nbVertexId = nbArr[i], nbEdgeId = nbArr[i + 1];
                    if (targetVType == vertexType[nbVertexId]
                            && targetEType == edgeType[nbEdgeId]
                            && edgeUsedTimes[nbEdgeId] > 0
                    ) {
                        // 记录起始点位置
                        if (index == 0) {
                            startPoint = anchorId;
                        }
                        // 如果这次找到的是目标类型节点
                        if (index == metaPath.pathLen - 1) {
                            if (flag) {
                                // 目的是为了找到没有找到过的点，因此如果发现此点已经被找到则跳过
                                if (vertexFound.contains(nbVertexId))
                                    continue;
                                // TODO: 通过度数限制进行剪枝
                                // 同时如果新的点的度数不足k - 1则直接去掉


                            } else {
                                // 只寻找在已知图中的边且必须是新找到的点集合中的
                                if (!foundVertex.contains(nbVertexId) || nbVertexId == startPoint) {
                                    continue;
                                }
                                // 如果同构图中已经有边则跳过
                                Set<Integer> vertexList = homoGraph.get(nbVertexId);
                                if (vertexList.contains(anchorId)) {
                                    continue;
                                }
                            }
                            if (keepSet.contains(nbVertexId)) {
                                nextBatchSet.add(nbVertexId);
                                recordPath(pathRecordMap, anchorId, nbVertexId);
                            }
                        } else {
                            nextBatchSet.add(nbVertexId);
                            recordPath(pathRecordMap, anchorId, nbVertexId);
                        }
                    }
                }
            }
            batchSet = nextBatchSet;
        }
        return batchSet;
    }

    private void createPathSet(Set<Integer> vertexSet, HashMap<Integer, ArrayList<Integer>> pathRecordMap) {
        // TODO: 减少生成路径集合的大小?
        // 重新初始化所有路径集合
        allPaths = new ArrayList<>();
        // 重新初始化边对应的受影响路径
        edgeMapPath = new HashMap<>();
        for (int vertexStart : vertexSet) {
            // 记录已经到达的点
            Set<Integer> reached = new HashSet<>();
            // 存储从单个点开始的所有路径
            Queue<ArrayList<Integer>> path = new LinkedList<>();
            Queue<Integer> queue = new LinkedList<>();
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(vertexStart);
            path.offer(temp);
            queue.offer(vertexStart);
            while (!queue.isEmpty()) {
                int tempVertex = queue.poll();
                ArrayList<Integer> tempPath = path.poll();
                if ((pathRecordMap.get(tempVertex) == null || reached.contains(tempVertex))) {
                    if (Objects.requireNonNull(tempPath).size() == pathLen) {
                        allPaths.add(tempPath);
                        // 遍历路径的所有边，在HashMap中以边为键映射到边
                        for (int i = 0; i < tempPath.size() - 1; i++) {
                            int k = tempPath.get(i);
                            int v = tempPath.get(i + 1);
                            int edgeId = vertexPairMapEdge.get(Map.entry(k, v));
                            if (edgeMapPath.containsKey(edgeId)) {
                                ArrayList<ArrayList<Integer>> tmpPathList = edgeMapPath.get(edgeId);
                                tmpPathList.add(tempPath);
                            } else {
                                ArrayList<ArrayList<Integer>> tempPathList = new ArrayList<>();
                                tempPathList.add(tempPath);
                                edgeMapPath.put(edgeId, tempPathList);
                            }
                        }
                    }
                } else {
                    ArrayList<Integer> tmpArr = pathRecordMap.get(tempVertex);
                    for (Integer integer : tmpArr) {
                        if (integer != vertexStart) {
                            if (vertexType[integer] == vertexType[vertexStart])
                                reached.add(integer);
                            queue.offer(integer);
                            assert tempPath != null;
                            ArrayList<Integer> newArr = new ArrayList<>(tempPath);
                            newArr.add(integer);
                            path.offer(newArr);
                        }
                    }
                }
            }
        }
    }

    private void recordPath(HashMap<Integer, ArrayList<Integer>> pathRecordMap, int anchorId, int nbVertexId) {
        if (!pathRecordMap.containsKey(anchorId)) {
            ArrayList<Integer> vertexLinker = new ArrayList<>();
            vertexLinker.add(nbVertexId);
            pathRecordMap.put(anchorId, vertexLinker);
        } else {
            ArrayList<Integer> vertexLinkerPointer = pathRecordMap.get(anchorId);
            vertexLinkerPointer.add(nbVertexId);
        }
    }
}