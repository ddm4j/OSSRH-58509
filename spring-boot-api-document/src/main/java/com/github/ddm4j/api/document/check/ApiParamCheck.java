package com.github.ddm4j.api.document.check;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.ddm4j.api.document.annotation.ApiParam;
import com.github.ddm4j.api.document.annotation.ApiParams;
import com.github.ddm4j.api.document.common.exception.ParamCheckError;
import com.github.ddm4j.api.document.common.exception.ParamCheckException;
import com.github.ddm4j.api.document.config.ApiDocumentConfig;

@Aspect
@Component
public class ApiParamCheck {

	@Autowired
	ApiDocumentConfig config;

	/**
	 * 全局校验
	 * 
	 * @author DDM 2019年11月27日
	 * @param jp
	 *            参数对象
	 * @param apiParams
	 *            注解
	 * @throws Exception
	 *             异常信息
	 */
	@Before("execution(public * *(..)) && @annotation(apiParams)")
	public void checkParam(JoinPoint jp, ApiParams apiParams) throws Exception {
		// System.out.println("开始校验");
		if (null == config || !config.isCheck()) {
			// 未开校验
			return;
		}
		ApiParam[] params = apiParams.value();
		if (null == params || params.length == 0) {
			// 未配置
			System.out.println("没有配置");
			return;
		}

		if (null == jp.getArgs() || jp.getArgs().length == 0) {
			System.out.println("没有参数");
			// 没有参数
			return;
		}
		// 判断是不是JSON
		boolean isJson = false;
		Integer index = null;
		MethodSignature signature = (MethodSignature) jp.getSignature();
		Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
		for (int i = 0; i < parameterAnnotations.length; i++) {
			for (Annotation annotation : parameterAnnotations[i]) {
				if (annotation instanceof RequestBody) {
					isJson = true;
					index = i;
					break;
				}
			}
			if (isJson) {
				break;
			}
		}

		if (isJson && index != null) {
			// 是JSON, 取出第一个参数
			Object param = jp.getArgs()[index];
			if (null == param) {
				for (ApiParam ap : apiParams.value()) {
					if (ap.required() == true) {
						throw new ParamCheckException(ap.field(), ParamCheckError.EMPTY, ap.describe(), "不能为空");
					}
				}
				return;
			}

			// 校验
			checkByJson(param, params);
		} else {
			// 普通校验
			// System.out.println("普通校验");
			LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
			String[] names = u.getParameterNames(signature.getMethod());
			for (int j = 0; j < names.length; j++) {
				checkParam(jp.getArgs()[j], names[j], params);
			}
		}
	}

	public static ApiParam getParam(ApiParam[] params, String name) {
		for (ApiParam apiParam : params) {
			if (apiParam.field().equals(name)) {
				return apiParam;
			}
		}
		return null;
	}

	public static void checkParam(Object param, String name, ApiParam[] apiParams)
			throws ParamCheckException, Exception {
		ApiParam apiParam = null;
		if (null == param) {
			apiParam = getParam(apiParams, name);
			if (apiParam.required()) {
				throw new ParamCheckException(apiParam.field(), ParamCheckError.EMPTY, apiParam.describe(), "不能为空");
			}
			return;
		}

		Integer checkType = checkParamType(param);

		switch (checkType) {
		case 1:// object;
				// SpringBoot 不能直接接收该类型的数据
			break;
		case 2:// Number Or String
			apiParam = getParam(apiParams, name);

			// 判断是不是，是不是，只用来描述，是直接跳过
			if (null == apiParam || (apiParam.required() == false && "".equals(apiParam.reg())
					&& apiParam.max() == 2147483647 && apiParam.min() == -2147483648)) {
				return;
			}

			if (null == param) {
				if (apiParam.required()) {
					throw new ParamCheckException(apiParam.field(), ParamCheckError.EMPTY, apiParam.describe(), "不能为空");
				}
				return;
			}

			checkJsonValue(param, apiParam);

			break;
		case 3:// Array
			apiParam = getParam(apiParams, name);

			// 判断是不是，是不是，只用来描述，是直接跳过
			if (null == apiParam || (apiParam.required() == false && "".equals(apiParam.reg())
					&& apiParam.max() == 2147483647 && apiParam.min() == -2147483648)) {
				return;
			}

			if (null == param) {
				if (apiParam.required()) {
					throw new ParamCheckException(apiParam.field(), ParamCheckError.EMPTY, apiParam.describe(), "不能为空");
				}
				return;
			}

			Object[] objs = exArray(param);
			if (null != objs && objs.length > 0) {
				for (Object object : objs) {
					checkJsonValue(object, apiParam);
				}
			}
			break;
		case 4:// List
				// SpringBoot 不能直接接收该类型的数据
			break;
		case 5:// Set
				// SpringBoot 不能直接接收该类型的数据
			break;
		case 6:// 自定义
			checkByJson(param, apiParams);
			break;
		}

	}

	private static Object[] exArray(Object param) {
		if (param instanceof Object[]) {
			return (Object[]) param;
		} else if (param instanceof int[]) {
			int[] ints = (int[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Integer temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		} else if (param instanceof byte[]) {
			byte[] ints = (byte[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Byte temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		} else if (param instanceof short[]) {
			short[] ints = (short[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Short temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		} else if (param instanceof long[]) {
			long[] ints = (long[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Long temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		} else if (param instanceof double[]) {
			double[] ints = (double[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Double temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		} else if (param instanceof float[]) {
			float[] ints = (float[]) param;

			Object[] objs = new Integer[ints.length];
			if (ints.length > 0) {
				for (int i = 0; i < ints.length; i++) {
					Float temp = ints[i];
					objs[i] = temp;
				}
			}
			return objs;
		}
		return null;
	}

	/**
	 * 检查类型
	 * 
	 * @param obj
	 *            对象
	 * @return 1 Object 2 Number Or String 3 Array 4 List 5 Set 6 自定义
	 */

	public static int checkParamType(Object obj) {
		// 判断是不是基本类型或 String类型
		if (obj.getClass().getTypeName().equals(Object.class.getTypeName())) {
			// Object 类型
			return 1;
		} else if (Number.class.isAssignableFrom(obj.getClass()) || obj.getClass() == String.class) {
			// 基本类型
			return 2;
		} else if (obj.getClass().isArray()) {
			// 数组
			return 3;
		} else if (List.class.isAssignableFrom(obj.getClass())) {
			// List 集合
			return 4;
		} else if (Set.class.isAssignableFrom(obj.getClass())) {
			// Set 集合
			return 5;
		} else {
			// 自定义类
			return 6;
		}
	}

	public static boolean isEmpty(String str) {
		if (null == str || "".equals(str)) {
			return true;
		}
		return false;
	}

	public static void checkByJson(Object param, ApiParam[] apiParams) throws Exception {
		JSONObject json = JSON.parseObject(JSON.toJSONString(param));
		System.out.println("开始JSON校验:" + json.toJSONString());
		for (ApiParam apiParam : apiParams) {
			// 判断是不是，是不是，只用来描述，是直接跳过
			if (null == apiParam || (apiParam.required() == false && "".equals(apiParam.reg())
					&& apiParam.max() == 2147483647 && apiParam.min() == -2147483648)) {
				continue;
			}

			String[] keys = apiParam.field().split("\\.");

			takenJsonValue(json, keys, 0, apiParam);
		}
	}

	private static void takenJsonValue(JSONObject json, String[] keys, int index, ApiParam param) {
		System.out.println("校验：" + param.field() + " key:" + keys[index]);

		if (!json.containsKey(keys[index]) || null == json.get(keys[index])) {
			if (param.required()) {
				throw new ParamCheckException(param.field(), ParamCheckError.EMPTY, param.describe(), "不能为空");
			}
			return;
		}

		if (index + 1 < keys.length) {
			// key 不存在，跳过
			if (!json.containsKey(keys[index])) {
				return;
			}
			// System.out.println("1 - " + json.getJSONObject(keys[index]).toJSONString());
			Object obj = json.get(keys[index]);
			if (obj.getClass().isArray() || obj instanceof List<?>) {
				JSONArray array = json.getJSONArray(keys[index]);
				System.out.println("1-- array ");
				for (int i = 0; i < array.size(); i++) {

					takenJsonValue(array.getJSONObject(i), keys, index + 1, param);
				}
			} else {
				System.out.println("1 - object " + json.getJSONObject(keys[index]).toJSONString());
				takenJsonValue(json.getJSONObject(keys[index]), keys, index + 1, param);
			}
		} else {
			// key 不存在，跳过
			if (!json.containsKey(keys[index])) {
				return;
			}
			Object value = json.get(keys[index]);

			System.out.println("3 - " + value.toString());

			if (value.getClass().isArray()) {
				System.out.println("3- array");
				Object[] values = (Object[]) value;
				for (Object obj : values) {
					checkJsonValue(obj, param);
				}
			} else if (value instanceof List<?>) {
				System.out.println("3- list");
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				for (Object obj2 : list) {
					// System.out.println("-- " + obj);
					checkJsonValue(obj2, param);
				}

			} else {
				System.out.println("3- value");
				checkJsonValue(value, param);
			}
		}
	}

	private static void checkJsonValue(Object value, ApiParam apiParam) {
		if (Number.class.isAssignableFrom(value.getClass())) {

			// 数字类型
			Double dou = Double.parseDouble(value.toString());

			if (dou < apiParam.min()) {
				throw new ParamCheckException(apiParam.field(), ParamCheckError.MIN, apiParam.describe(),
						"不能低于" + apiParam.min());
			}

			if (dou > apiParam.max()) {
				throw new ParamCheckException(apiParam.field(), ParamCheckError.MAX, apiParam.describe(),
						"不能高于" + apiParam.max());
			}

			if (!isEmpty(apiParam.reg())) {
				if (!value.toString().matches(apiParam.reg())) {
					throw new ParamCheckException(apiParam.field(), ParamCheckError.REGULAR, apiParam.describe(),
							"正则校验失败");
				}
			}

		} else if (value.getClass() == String.class) {
			String str = value.toString().trim();

			if (str.length() < apiParam.min()) {
				throw new ParamCheckException(apiParam.field(), ParamCheckError.MIN, apiParam.describe(),
						"长度不能低于" + apiParam.min());
			}

			if (str.length() > apiParam.max()) {
				throw new ParamCheckException(apiParam.field(), ParamCheckError.MAX, apiParam.describe(),
						"长度不能高于" + apiParam.max());
			}

			// 字符串类型
			if (!isEmpty(apiParam.reg())) {
				if (!str.matches(apiParam.reg())) {
					throw new ParamCheckException(apiParam.field(), ParamCheckError.REGULAR, apiParam.describe(),
							"正则校验失败");
				}
			}

		} else {
			// 未找到
		}
	}

}
