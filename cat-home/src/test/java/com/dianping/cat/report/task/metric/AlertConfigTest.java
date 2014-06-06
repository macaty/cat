package com.dianping.cat.report.task.metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.unidal.helper.Files;
import org.unidal.tuple.Pair;

import com.dianping.cat.Cat;
import com.dianping.cat.advanced.metric.config.entity.MetricItemConfig;
import com.dianping.cat.home.rule.entity.Condition;
import com.dianping.cat.home.rule.entity.Config;
import com.dianping.cat.home.rule.entity.MonitorRules;
import com.dianping.cat.home.rule.entity.Rule;
import com.dianping.cat.home.rule.entity.Subcondition;
import com.dianping.cat.home.rule.transform.DefaultSaxParser;
import com.dianping.cat.report.task.alert.DataChecker;
import com.dianping.cat.report.task.alert.DefaultDataChecker;

public class AlertConfigTest {

	private DataChecker m_checker = new DefaultDataChecker();

	private Map<String, List<com.dianping.cat.home.rule.entity.Config>> buildConfigMap(MonitorRules monitorRules) {
		if (monitorRules == null || monitorRules.getRules().size() == 0) {
			return null;
		}

		Map<String, List<com.dianping.cat.home.rule.entity.Config>> map = new HashMap<String, List<com.dianping.cat.home.rule.entity.Config>>();

		for (Rule rule : monitorRules.getRules()) {
			map.put(rule.getId(), rule.getConfigs());
		}

		return map;
	}

	private MonitorRules buildMonitorRuleFromFile(String path) {
		try {
			String content = Files.forIO().readFrom(this.getClass().getResourceAsStream(path), "utf-8");
			return DefaultSaxParser.parse(content);
		} catch (Exception ex) {
			Cat.logError(ex);
			return null;
		}
	}
	
	private List<Config> convert(MetricItemConfig metricItemConfig) {
		List<Config> configs = new ArrayList<Config>();
		Config config = new Config();
		Condition condition = new Condition();
		Subcondition descPerSubcon = new Subcondition();
		Subcondition descValSubcon = new Subcondition();

		double decreasePercent = metricItemConfig.getDecreasePercentage();
		double decreaseValue = metricItemConfig.getDecreaseValue();

		if (decreasePercent == 0) {
			decreasePercent = 50;
		}
		if (decreaseValue == 0) {
			decreaseValue = 100;
		}

		descPerSubcon.setType("DescPer").setText(String.valueOf(decreasePercent));
		descValSubcon.setType("DescVal").setText(String.valueOf(decreaseValue));

		condition.addSubcondition(descPerSubcon).addSubcondition(descValSubcon);
		config.addCondition(condition);
		configs.add(config);
		return configs;
	}


	@Test
	public void test() {
		DataChecker alertConfig = new DefaultDataChecker();
		MetricItemConfig itemConfig = new MetricItemConfig();
		List<Config> configs = convert(itemConfig);

		double baseline[] = { 100, 100 };
		double value[] = { 200, 200 };
		Pair<Boolean, String> result = alertConfig.checkData(value, baseline, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		double[] baseline2 = { 100, 100 };
		double[] value2 = { 49, 49 };
		result = alertConfig.checkData(value2, baseline2, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		double[] baseline3 = { 100, 100 };
		double[] value3 = { 51, 49 };
		result = alertConfig.checkData(value3, baseline3, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		double[] baseline4 = { 50, 50 };
		double[] value4 = { 10, 10 };
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		itemConfig.setDecreaseValue(40);
		itemConfig.setDecreasePercentage(50);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), true);

		itemConfig.setDecreaseValue(41);
		itemConfig.setDecreasePercentage(50);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		itemConfig.setDecreaseValue(40);
		itemConfig.setDecreasePercentage(79);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), true);

		itemConfig.setDecreaseValue(40);
		itemConfig.setDecreasePercentage(80);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		itemConfig.setDecreaseValue(40);
		itemConfig.setDecreasePercentage(80);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value4, baseline4, configs);
		Assert.assertEquals(result.getKey().booleanValue(), false);

		double[] baseline5 = { 117, 118 };
		double[] value5 = { 43, 48 };
		itemConfig.setDecreasePercentage(50);
		itemConfig.setDecreasePercentage(50);
		configs = convert(itemConfig);
		result = alertConfig.checkData(value5, baseline5, configs);
		Assert.assertEquals(result.getKey().booleanValue(), true);
	}

	@Test
	public void testMinute() {
		Map<String, List<com.dianping.cat.home.rule.entity.Config>> configMap = buildConfigMap(buildMonitorRuleFromFile("/config/test-minute-monitor.xml"));

		Assert.assertNotNull(configMap);

		double baseline[] = { 50, 200, 200 };
		double value[] = { 50, 100, 100 };
		Pair<Boolean, String> result = m_checker.checkData(value, baseline, configMap.get("two-minute"));
		Assert.assertEquals(result.getKey().booleanValue(), true);
	}

	@Test
	public void testRule() {
		Map<String, List<com.dianping.cat.home.rule.entity.Config>> configMap = buildConfigMap(buildMonitorRuleFromFile("/config/test-rule-monitor.xml"));

		Assert.assertNotNull(configMap);

		double baseline[] = { 200, 200 };
		double value[] = { 100, 100 };
		Pair<Boolean, String> result = m_checker.checkData(value, baseline, configMap.get("decreasePercentage"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline2 = { 200, 300 };
		double[] value2 = { 100, 100 };
		result = m_checker.checkData(value2, baseline2, configMap.get("decreaseValue"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline3 = { 200, 50 };
		double[] value3 = { 400, 100 };
		result = m_checker.checkData(value3, baseline3, configMap.get("increasePercentage"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline4 = { 200, 50 };
		double[] value4 = { 400, 100 };
		result = m_checker.checkData(value4, baseline4, configMap.get("increaseValue"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline5 = { 200, 200 };
		double[] value5 = { 500, 600 };
		result = m_checker.checkData(value5, baseline5, configMap.get("absoluteMaxValue"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline6 = { 200, 200 };
		double[] value6 = { 50, 40 };
		result = m_checker.checkData(value6, baseline6, configMap.get("absoluteMinValue"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline7 = { 200, 200 };
		double[] value7 = { 100, 100 };
		result = m_checker.checkData(value7, baseline7, configMap.get("conditionCombination"));
		Assert.assertEquals(result.getKey().booleanValue(), true);

		double[] baseline8 = { 200, 200 };
		double[] value8 = { 100, 100 };
		result = m_checker.checkData(value8, baseline8, configMap.get("subconditionCombination"));
		Assert.assertEquals(result.getKey().booleanValue(), false);
	}
}
