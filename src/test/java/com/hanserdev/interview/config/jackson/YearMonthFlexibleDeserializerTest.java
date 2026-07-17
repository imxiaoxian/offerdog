package com.hanserdev.interview.config.jackson;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanserdev.interview.config.CustomJacksonConfiguration;
import com.hanserdev.interview.utils.MarkdownUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
class YearMonthFlexibleDeserializerTest {

    private final ObjectMapper objectMapper = new CustomJacksonConfiguration().objectMapper();

    @Resource
    private ChatClient chatClient;

    @Test
    void shouldDeserializeFromStringValue() throws Exception {
        String payload = "\"2024-05\"";
        YearMonth yearMonth = objectMapper.readValue(payload, YearMonth.class);
        assertEquals(YearMonth.of(2024, 5), yearMonth);
    }

    @Test
    void shouldDeserializeFromObjectValue() throws Exception {
        String payload = "{\"year\":2023,\"month\":11}";
        YearMonth yearMonth = objectMapper.readValue(payload, YearMonth.class);
        assertEquals(YearMonth.of(2023, 11), yearMonth);
    }

    @Test
    void testMarkdownToPlainText() {
        String prompt = """
                李明你好，感谢你的介绍。很高兴能和您聊聊这方面的经验。
                
                                             关于数据库执行 SQL 查询的内部流程，我将以我们**实时推荐系统**中一个典型的查询场景——**根据用户ID获取个性化特征**——为例进行解释。
                
                                             ---
                
                                             ### 🚀 **数据库查询执行的内部流程**
                
                                             整个流程可以概括为三个主要阶段：**解析（Parsing）**、**优化（Optimization）**和**执行（Execution）**。
                
                                             #### 1. 解析阶段 (Parser)
                
                                             * **输入处理:** 数据库接收到 SQL 查询语句，例如 `SELECT features FROM user_profile WHERE user_id = 'user123'`。
                                             * **词法分析 (Lexer):** 将 SQL 语句拆分成一个个有意义的单元（Token），如 `SELECT`, `features`, `FROM`, `user_profile`, `WHERE`, `=`, `'user123'`。
                                             * **语法分析 (Parser):** 根据数据库的语法规则，将这些 Token 组织成一个树形结构，称为**抽象语法树 (AST)**。这一步会检查 SQL 语句是否符合语法。
                                             * **语义分析:** 检查 AST 中的表名、列名是否存在，以及数据类型是否匹配等。
                
                                             #### 2. 优化阶段 (Optimizer)
                
                                             这是最核心的阶段，由**查询优化器**负责。它的目标是找到执行这个查询的**最高效**方式。
                
                                             * **逻辑优化:**
                                                 * 例如，进行**谓词下推**（将过滤条件尽早执行）、**常量折叠**等，减少需要处理的数据量。
                                             * **物理优化：选择执行路径（Execution Plan）**
                                                 * 优化器会估算所有可能的执行路径的**成本**（Cost，通常是I/O次数和CPU消耗的函数）。
                                                 * 它依赖于**统计信息 (Statistics)**，如表的大小、索引的选择性（Selectivity，即索引区分度的程度）等。
                                                 * **索引路径选择：** 这是关键。
                                                     * 对于我们的推荐系统查询：`WHERE user_id = 'user123'`。如果 `user_id` 列上有**主键索引或唯一索引**，优化器几乎一定会选择这个索引进行**“Index Seek”**（精确查找），因为它的效率最高，成本最低。
                                                     * 如果字段上没有合适的索引，或者查询条件不适合索引（如对非索引列进行函数操作），优化器就可能选择成本更高的**“全表扫描 (Full Table Scan)”**。
                
                                             #### 3. 执行阶段 (Executor)
                
                                             * 优化器选择出成本最低的**执行计划**后，执行器（或执行引擎）开始工作。
                                             * 执行器根据执行计划中的操作步骤，调用底层的存储引擎接口来读取和操作数据。
                                             * 操作通常包括：**索引查找 (Index Lookups)**、**数据读取 (Data Fetch)**、**连接操作 (Joins)**、**排序 (Sorts)**、**过滤 (Filters)**等。
                                             * **结果返回:** 最终将查询结果（在这个例子中是用户特征 `features`）返回给应用层。
                
                                             ---
                
                                             ### 💡 结合推荐系统场景的说明
                
                                             在我们的推荐系统中，`user_profile` 表的查询对**延迟 (Latency)**要求极高，因为它是实时特征获取的关键。
                
                                             1.  **索引关键性:** 我们必须确保 `user_profile` 表上的 `user_id` 列是**主键**。这样优化器才能确定地选择最高效的**主键索引查找**路径，将查询时间锁定在毫秒级。如果优化器错误选择了全表扫描，在高并发下系统就会瞬间崩溃。
                                             2.  **统计信息维护:** 我们需要定期（或自动）维护数据库的统计信息，确保优化器对 `user_id` 的分布有准确的了解，防止优化器因为统计信息过期而选择次优的执行计划。
                                             3.  **避免复杂查询:** 实时查询场景通常避免复杂的 JOIN 或子查询，确保执行计划的**确定性和简单性**，以保证查询性能的稳定性。
                
                                             这是我对此问题的理解和在实际工作中的应用。
                
                                             ---
                
                                             我们刚才聊了查询优化器如何选择单个表的索引路径。接下来，您希望我们讨论一下：在**高并发**环境下，为了进一步提升特征查询性能，我们是如何通过**分库分表**来优化存储和查询的吗？
                """;
        String s = prompt
                .replace("#", "")
                .replace("*", "")
                .replace("-", "")
                .replaceAll("(?m)^[ \t]*\r?\n", "");// 移除纯空白行
        String plainText = MarkdownUtils.toPlainText(s);
        System.out.println(plainText);
    }


}
