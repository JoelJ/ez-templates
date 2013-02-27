TemplateImplementationPage = {
	hideConfiguration: function() {
		$('templateImplementationWarning').addClassName("show");
	},

	showConfiguration: function() {
		$('templateImplementationWarning').removeClassName("show");
	},

	checkboxChecked: function(event) {
		var checkbox = event.target;
		if(checkbox.checked) {
			TemplateImplementationPage.hideConfiguration();
		} else {
			TemplateImplementationPage.showConfiguration();
		}
	}
};

Event.observe(window, 'load', function() {
	var implementTemplateCheckbox = $$('input[name="useTemplate"]').first();
	if(implementTemplateCheckbox.checked) {
		TemplateImplementationPage.hideConfiguration();
	}

	implementTemplateCheckbox.observe('click', TemplateImplementationPage.checkboxChecked);
});
