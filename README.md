# Liferay Audience Targeting - Custom Rule Example

An example OSGI module that demonstrates how to create a custom Liferay Audience Targeting rule 

## Dependencies

- [Liferay DXP 7.0+](http://www.liferay.com)
- [OpenWeatherMap](https://openweathermap.org) API key

## Instructions

Go to openweathermap.org, sign up to get your API key and place it in portlet.properties.

Once deployed, this module creates a new custom rule under the category "Sample". The rule allows an administrator to select the desired weather condition.
![Alt text](img/custom_rule.png?raw=true "Liferay Audience Targeting Custom Rule")

The evaluate() method of this module retrieves the city from the user's profile and subsequently, through the OpenWeatherMap API, establishes the current weather for that city.

With a matching weather condition, personalised content can be delivered to the user.
![Alt text](img/sunglasses_offer_banner.png?raw=true "Liferay Content Personalisation")
   