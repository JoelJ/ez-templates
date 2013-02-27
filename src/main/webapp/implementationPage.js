TemplateImplementationPage = {
	keepVisibleFields: [
		'[name="useTemplate"]',
		'[name="name"]',
		'[name="parameterized"]',
//		'.section-header',
		'[name*="Trigger"]',
		'[name*="trigger"]',
		'[name="parameter"]',
		'.apply-button',
		'[name="description"]',
		'[name="_.templateJobName"]'
	],

	hideConfiguration: function() {
		console.log("hiding configuration");
		$$('form[name="config"]').first().addClassName('disabled')
	},

	showConfiguration: function() {
		console.log("showing configuration");
		$$('form[name="config"]').first().removeClassName('disabled')
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

	TemplateImplementationPage.keepVisibleFields.forEach(function(selector) {
		var selected = $$(selector);
		if(selected) {
			selected.forEach(function(element) {
				element.up('tr').addClassName('enabled');
			});
		}
	});

	$$('input[name="pseudoUpstreamTrigger"]').first().up('tr').previousSibling.addClassName('enabled');
});
