package common;

import models.MetaPath;

import java.util.HashSet;
import java.util.Set;

/**
 * @author
 * @date
 * Given a vertex (or a set of vertices), find its or (their) connected neighbors
 */
public class BatchSearch {
	private int graph[][] = null;//data graph, including vertex IDs, edge IDs, and their link relationships
	private int vertexType[] = null;//vertex -> type
	private int edgeType[] = null;//edge -> type
	private MetaPath queryMPath = null;
	
	public BatchSearch(int[][] graph, int[] vertexType, int[] edgeType, MetaPath queryMPath) {
		this.graph = graph;
		this.vertexType = vertexType;
		this.edgeType = edgeType;
		this.queryMPath = queryMPath;
	}

	//给一个集合Q,找到与集合Q中的节点为P-邻居的集合
	public Set<Integer> collect(Set<Integer> startSet, Set<Integer> keepSet) {
		int pathLen = queryMPath.pathLen;
		Set<Integer> batchSet = startSet;

		for(int index = 0;index < pathLen;index ++) {
			int targetVType = queryMPath.vertex[index + 1], targetEType = queryMPath.edge[index];

			Set<Integer> nextBatchSet = new HashSet<Integer>();
			for(int anchorId:batchSet) {
				int[] nbArr = graph[anchorId];
				for(int i = 0;i < nbArr.length;i += 2) {
					int nbVertexID = nbArr[i], nbEdgeID = nbArr[i + 1];
					if(targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
						if(index == queryMPath.pathLen - 1) {//impose restriction
							if(keepSet.contains(nbVertexID)) {
								nextBatchSet.add(nbVertexID);
							}
						}else {
							nextBatchSet.add(nbVertexID);
						}
					}
				}
			}
			batchSet = nextBatchSet;
		}
		return batchSet;
	}

	//给定一个节点q,找出与他P-邻居节点集S
	public Set<Integer> collect(int startId, Set<Integer> keepSet) {
		Set<Integer> anchorSet = new HashSet<Integer>();
		anchorSet.add(startId);
		
		for(int layer = 0;layer < queryMPath.pathLen;layer ++) {
			int targetVType = queryMPath.vertex[layer + 1], targetEType = queryMPath.edge[layer];
			
			Set<Integer> nextAnchorSet = new HashSet<Integer>();
			for(int anchorId:anchorSet) {
				int[] nb = graph[anchorId];
				for(int i = 0;i < nb.length;i += 2) {
					// 找出节点id与边id
					int nbVertexID = nb[i], nbEdgeID = nb[i + 1];
					if(targetVType == vertexType[nbVertexID] && targetEType == edgeType[nbEdgeID]) {
						if(layer < queryMPath.pathLen - 1) {
							nextAnchorSet.add(nbVertexID);
						}else {
							if(keepSet.contains(nbVertexID))   nextAnchorSet.add(nbVertexID);//impose restriction
						}
					}
				}
			}
			anchorSet = nextAnchorSet;
		}
		
		anchorSet.remove(startId);//2018-9-19-bug: remove the duplicated vertex 删除重复节点
		return anchorSet;
	}
}
