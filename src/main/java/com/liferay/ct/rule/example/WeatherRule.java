package com.liferay.ct.rule.example;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.liferay.content.targeting.anonymous.users.model.AnonymousUser;
import com.liferay.content.targeting.api.model.BaseJSPRule;
import com.liferay.content.targeting.api.model.Rule;
import com.liferay.content.targeting.model.RuleInstance;
import com.liferay.content.targeting.rule.categories.SampleRuleCategory;
import com.liferay.portal.kernel.configuration.Configuration;
import com.liferay.portal.kernel.configuration.ConfigurationFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Address;
import com.liferay.portal.kernel.model.Contact;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.AddressLocalServiceUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.StringPool;

/**
 * @author jfeeney
 */

@Component(immediate = true, service = Rule.class)
public class WeatherRule extends BaseJSPRule {

	@Activate
	@Override
	public void activate() {
		super.activate();
	}

	@Deactivate
	@Override
	public void deActivate() {
		super.deActivate();
	}

	@Override
	public boolean evaluate(
			HttpServletRequest httpServletRequest, RuleInstance ruleInstance,
			AnonymousUser anonymousUser) throws Exception {
		
		String typeSettings = ruleInstance.getTypeSettings();
		
		String weather = getUserWeather(anonymousUser);
		
		_log.debug("CT rule: [selection = " + typeSettings + "][weather = " + weather + "]");
		
		if (typeSettings.equalsIgnoreCase(weather)) {
			return true;
		}

		return false;
	}

	@Override
	public String getIcon() {
		return "icon-puzzle-piece";
	}

	@Override
	public String getRuleCategoryKey() {

		return SampleRuleCategory.KEY;
	}

	@Override
	public String getSummary(RuleInstance ruleInstance, Locale locale) {
		return ruleInstance.getTypeSettings();
	}

	@Override
	public String processRule(
			PortletRequest portletRequest, PortletResponse portletResponse,
			String id, Map<String, String> values) {
		
		return values.get("weather");
	}

	@Override
	@Reference(
			target = "(osgi.web.symbolicname=ct-weather-rule)",
			unbind = "-"
			)
	public void setServletContext(ServletContext servletContext) {
		super.setServletContext(servletContext);
	}

	@Override
	protected void populateContext(
			RuleInstance ruleInstance, Map<String, Object> context,
			Map<String, String> values) {

		String weather = "";

		if (!values.isEmpty()) {
			weather = GetterUtil.getString(values.get("weather"));
		}
		else if (ruleInstance != null) {
			weather = ruleInstance.getTypeSettings();
		}

		context.put("weather", weather);
	}

	protected String getCityFromUserProfile(long contactId, long companyId)
			throws PortalException, SystemException {

		List<Address> addresses = AddressLocalServiceUtil.getAddresses(companyId, Contact.class.getName(), contactId);

		if (addresses.isEmpty()) {
			return null;
		}

		Address address = addresses.get(0);

		return address.getCity() + StringPool.COMMA + address.getCountry().getA2();		
	}	

	protected String getUserWeather(AnonymousUser anonymousUser)
			throws PortalException, SystemException {

		User user = anonymousUser.getUser();
		String city = getCityFromUserProfile(user.getContactId(), user.getCompanyId());

		Http.Options options = new Http.Options();

		String apiUrl = _configuration.get("weather.api.url");
		String apiKey = _configuration.get("weather.api.key");
		
		String location = HttpUtil.addParameter(apiUrl, "q", city);
		location = HttpUtil.addParameter(location, "format", "json");
		location = HttpUtil.addParameter(location, "APPID", apiKey);
		
		options.setLocation(location);

		JSONArray weatherArray = null;

		try {
			String text = HttpUtil.URLtoString(options);
			JSONObject jsonObject = JSONFactoryUtil.createJSONObject(text);			
			weatherArray = jsonObject.getJSONArray("weather");			
		}
		catch (Exception e) {
			_log.error(e);
		}

		return weatherArray != null ? getWeatherFromCodes(weatherArray) : "No expected value returned";
	}

	protected String getWeatherFromCodes(JSONArray weatherArray) {
		
		for (int i = 0; i < weatherArray.length(); i++ ) {
			
			int code = weatherArray.getJSONObject(i).getInt("id");
			
			if (code == 800 || code == 801) {
				return "sunny";
			}
			else if (code > 801 && code < 805) {
				return "clouds";
			}
			else if (code >= 600 && code < 622) {
				return "snow";
			}
			else if (code >= 500 && code < 532) {
				return "rain";
			}
			else if (code >= 300 && code < 322) {
				return "drizzle";
			}			
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(WeatherRule.class);
	private static final Configuration _configuration = 
			ConfigurationFactoryUtil.getConfiguration(WeatherRule.class.getClassLoader(), "portlet");
}