import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GraphProcessor {
    static Graph graph; // 静态图对象，用于其他函数访问

    // 主程序入口
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("请提供文本文件路径，例如: java GraphProcessor path/to/file.txt");
            return;
        }

        String filePath = args[0]; // 从命令行参数获取文件路径
        try {
            // 读取并处理文本文件
            String text = readAndProcessFile(filePath);
            graph = buildGraph(text); // 生成有向图

            System.out.println("图结构已生成。请选择功能：");
            System.out.println("1: 展示有向图");
            System.out.println("2: 查询桥接词");
            System.out.println("3: 生成新文本");
            System.out.println("4: 计算最短路径");
            System.out.println("5: 计算PageRank");
            System.out.println("6: 随机游走");
            System.out.println("7: 退出");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("输入功能编号: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // 消耗换行

                switch (choice) {
                    case 1:
                        showDirectedGraph(graph); // 展示图
                        break;
                    case 2:
                        System.out.print("输入word1: ");
                        String word1 = scanner.nextLine().toLowerCase();
                        System.out.print("输入word2: ");
                        String word2 = scanner.nextLine().toLowerCase();
                        String bridgeResult = queryBridgeWords(word1, word2);
                        System.out.println(bridgeResult); // 输出桥接词结果
                        break;
                    case 3:
                        System.out.print("输入新文本: ");
                        String inputText = scanner.nextLine();
                        String newText = generateNewText(inputText);
                        System.out.println("生成的新文本: " + newText); // 输出新文本
                        break;
                    case 4:
                        System.out.print("输入word1: ");
                        String spWord1 = scanner.nextLine().toLowerCase();
                        System.out.print("输入word2: ");
                        String spWord2 = scanner.nextLine().toLowerCase();
                        String shortestPath = calcShortestPath(spWord1, spWord2);
                        System.out.println(shortestPath); // 输出最短路径
                        break;
                    case 5:
                        // 新增：计算并输出所有节点的PageRank
                        System.out.println("正在计算所有节点的PageRank值...");
                        Map<String, Double> allPageRanks = new HashMap<>();
                        List<String> nodes = new ArrayList<>(graph.adjacencyList.keySet());  // 获取所有节点
                        
                        for (String node : nodes) {
                            double prValue = calPageRank(node);  // 为每个节点调用calPageRank
                            allPageRanks.put(node, prValue);
                            System.out.println("计算进度: 节点 " + node + " 已完成。");  // 可选进度提示
                        }
                        
                        // 输出所有节点的PageRank
                        System.out.println("所有节点的PageRank值:");
                        nodes.sort(String::compareTo);  // 按节点名称排序
                        for (String node : nodes) {
                            System.out.println("节点: " + node + "，PageRank值: " + allPageRanks.get(node));
                        }
                        break;
                    case 6:
                        String walkResult = randomWalk(); // 随机游走
                        System.out.println("随机游走路径: " + walkResult);
                        // 将结果写入文件
                        try (FileWriter writer = new FileWriter("random_walk.txt")) {
                            writer.write(walkResult);
                            System.out.println("路径已保存到 random_walk.txt");
                        } catch (IOException e) {
                            System.out.println("保存文件失败: " + e.getMessage());
                        }
                        break;
                    case 7:
                        System.out.println("退出程序。");
                        return;
                    default:
                        System.out.println("无效选择。");
                }
            }
        } catch (IOException e) {
            System.out.println("文件读取错误: " + e.getMessage());
        }
    }

    // 读取并处理文本文件
    private static String readAndProcessFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 替换非字母字符为空格
                line = line.replaceAll("[^a-zA-Z\\s]", " ")
                        .replaceAll("\\s+", " ") // 多个空格合并
                        .toLowerCase(); // 转换为小写
                sb.append(line).append(" ");
            }
        }
        return sb.toString().trim(); // 返回处理后的文本
    }

    // 生成有向图
    private static Graph buildGraph(String text) {
        Graph g = new Graph();
        String[] words = text.split("\\s+"); // 按空格分割单词
        for (int i = 0; i < words.length - 1; i++) {
            String source = words[i];
            String target = words[i + 1];
            if (!source.isEmpty() && !target.isEmpty()) {
                g.addEdge(source, target); // 添加边
            }
        }
        return g;
    }

    // 展示有向图（void showDirectedGraph(Graph G, ...)，这里无额外参数）
    public static void showDirectedGraph(Graph G) {
        // 先在CLI中自定义格式展示
        System.out.println("有向图结构:");
        for (String source : G.adjacencyList.keySet()) {
            Map<String, Integer> neighbors = G.adjacencyList.get(source);
            for (String target : neighbors.keySet()) {
                System.out.println(source + " → " + target + " (权重: " + neighbors.get(target) + ")");
            }
        }
        
        // 新增功能：将图保存为图形文件
        try {
            // 1. 生成DOT格式字符串
            String dotString = generateDotString(G);
            
            // 2. 写入DOT文件
            String dotFilePath = "graph.dot";
            try (FileWriter writer = new FileWriter(dotFilePath)) {
                writer.write(dotString);
            }
            System.out.println("DOT文件已生成: " + dotFilePath);
            
            // 3. 调用Graphviz生成图形文件
            String outputImagePath = "output_graph.png";  // 输出PNG文件
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", outputImagePath);
            pb.redirectErrorStream(true);  // 重定向错误流
            Process process = pb.start();
            int exitCode = process.waitFor();  // 等待进程完成
            
            if (exitCode == 0) {
                System.out.println("有向图已成功保存为图形文件: " + outputImagePath);
            } else {
                System.out.println("生成图形文件失败。Graphviz可能未安装或配置错误。退出码: " + exitCode);
                // 读取错误输出
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println(line);  // 输出错误信息
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("保存图形文件时出错: " + e.getMessage());
        }
    }

    // 辅助函数：生成DOT格式字符串
    private static String generateDotString(Graph G) {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph G {\n");  // 开始DOT文件
        for (String source : G.adjacencyList.keySet()) {
            Map<String, Integer> neighbors = G.adjacencyList.get(source);
            for (String target : neighbors.keySet()) {
                int weight = neighbors.get(target);
                dot.append("  \"").append(source).append("\" -> \"").append(target).append("\" [label=\"").append(weight).append("\"];\n");
            }
        }
        dot.append("}\n");  // 结束DOT文件
        return dot.toString();
    }

    // 查询桥接词（String queryBridgeWords(String word1, String word2)）
    public static String queryBridgeWords(String word1, String word2) {
        if (!graph.adjacencyList.containsKey(word1) || !graph.adjacencyList.containsKey(word2)) {
            return "No " + word1 + " or " + word2 + " in the graph!";
        }

        Set<String> bridges = new HashSet<>();
        Map<String, Integer> neighborsOfWord1 = graph.adjacencyList.getOrDefault(word1, new HashMap<>());

        for (String word3 : neighborsOfWord1.keySet()) {
            if (graph.adjacencyList.containsKey(word3) && graph.adjacencyList.get(word3).containsKey(word2)) {
                bridges.add(word3);
            }
        }

        if (bridges.isEmpty()) {
            return "No bridge words from " + word1 + " to " + word2 + "!";
        } else {
            return "The bridge words from " + word1 + " to " + word2 + " are: " +
                    String.join(", ", bridges) + ".";
        }
    }

    // 生成新文本（String generateNewText(String inputText)）
    public static String generateNewText(String inputText) {
        String[] words = inputText.toLowerCase().split("\\s+");
        StringBuilder newText = new StringBuilder();

        for (int i = 0; i < words.length - 1; i++) {
            String current = words[i];
            String next = words[i + 1];
            newText.append(current).append(" ");

            String bridges = queryBridgeWords(current, next); // 查询桥接词
            if (bridges.contains("are:")) { // 有桥接词
                String[] bridgeWords = bridges.substring(bridges.indexOf("are:") + 5).replace(".", "").split(",\\s*");
                if (bridgeWords.length > 0) {
                    Random rand = new Random();
                    String randomBridge = bridgeWords[rand.nextInt(bridgeWords.length)];
                    newText.append(randomBridge).append(" "); // 随机插入
                }
            }
        }
        if (words.length > 0)
            newText.append(words[words.length - 1]); // 添加最后一个单词
        return newText.toString().trim();
    }

    // 计算最短路径（String calcShortestPath(String word1, String word2)）
    public static String calcShortestPath(String word1, String word2) {
        if (!graph.adjacencyList.containsKey(word1) || !graph.adjacencyList.containsKey(word2)) {
            return "No path: " + word1 + " or " + word2 + " not in graph!";
        }

        // 首先，获取图中的所有唯一节点（包括源和目标节点）
        Set<String> allNodes = new HashSet<>(graph.adjacencyList.keySet()); // 添加所有源节点
        for (Map<String, Integer> neighbors : graph.adjacencyList.values()) {
            allNodes.addAll(neighbors.keySet()); // 添加所有目标节点
        }

        Map<String, Double> distances = new HashMap<>(); // Dijkstra距离Map
        Map<String, String> previous = new HashMap<>(); // 跟踪前驱节点
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(distances::get)); // 注意：这里可能仍需小心，但我们已初始化所有节点

        // 初始化distances Map，确保所有节点都有初始值
        for (String node : allNodes) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(word1, 0.0); // 起点距离为0
        pq.add(word1); // 添加起点到队列

        while (!pq.isEmpty()) {
            String current = pq.poll(); // 获取当前节点
            for (Map.Entry<String, Integer> neighbor : graph.adjacencyList.getOrDefault(current, new HashMap<>())
                    .entrySet()) {
                String nextNode = neighbor.getKey();
                double alt = distances.get(current) + neighbor.getValue(); // 计算备选距离
                if (alt < distances.get(nextNode)) { // 现在distances.get(nextNode)不会是null
                    distances.put(nextNode, alt);
                    previous.put(nextNode, current);
                    pq.add(nextNode); // 添加到队列
                }
            }
        }

        if (distances.get(word2) == Double.POSITIVE_INFINITY) {
            return "No path from " + word1 + " to " + word2 + "!";
        }

        // 构建路径字符串
        List<String> path = new ArrayList<>();
        String current = word2;
        while (current != null) {
            path.add(0, current);
            current = previous.get(current);
        }
        return "最短路径: " + String.join(" → ", path) + " (长度: " + distances.get(word2) + ")";
    }

    // 计算PageRank（Double calPageRank(String word)）
    public static double calPageRank(String word) {
        if (!graph.adjacencyList.containsKey(word))
            return 0.0;

        int numNodes = graph.adjacencyList.size();
        Map<String, Double> pr = new HashMap<>(); // 初始PR值基于单词频率
        for (String node : graph.adjacencyList.keySet())
            pr.put(node, 1.0 / numNodes); // 基础初始值

        double d = 0.85;
        for (int iter = 0; iter < 100; iter++) { // 迭代100次
            Map<String, Double> newPr = new HashMap<>();
            for (String node : graph.adjacencyList.keySet()) {
                double sum = 0.0;
                for (String incoming : graph.adjacencyList.keySet()) {
                    if (graph.adjacencyList.get(incoming).containsKey(node)) {
                        int outDegree = graph.adjacencyList.get(incoming).size();
                        sum += pr.get(incoming) / outDegree;
                    }
                }
                newPr.put(node, (1 - d) + d * sum);
            }
            pr = newPr;
        }
        return pr.getOrDefault(word, 0.0);
    }

    // 随机游走（String randomWalk()）
    public static String randomWalk() {
        if (graph.adjacencyList.isEmpty())
            return "图为空，无法游走。";

        Random rand = new Random();
        List<String> nodes = new ArrayList<>(graph.adjacencyList.keySet());
        String start = nodes.get(rand.nextInt(nodes.size())); // 随机起点
        String current = start;
        Set<String> visitedEdges = new HashSet<>(); // 跟踪边
        StringBuilder path = new StringBuilder(current);

        while (true) {
            Map<String, Integer> neighbors = graph.adjacencyList.getOrDefault(current, new HashMap<>());
            if (neighbors.isEmpty())
                break; // 无出边

            List<String> neighborList = new ArrayList<>(neighbors.keySet());
            String next = neighborList.get(rand.nextInt(neighborList.size())); // 随机选择下一个
            String edge = current + "→" + next;

            if (visitedEdges.contains(edge))
                break; // 重复边
            visitedEdges.add(edge);

            path.append(" ").append(next);
            current = next;
        }

        return path.toString();
    }

    // Graph类定义
    static class Graph {
        Map<String, Map<String, Integer>> adjacencyList = new HashMap<>();

        void addEdge(String source, String target) {
            adjacencyList.computeIfAbsent(source, k -> new HashMap<>()).merge(target, 1, Integer::sum);
        }
    }
}