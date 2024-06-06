/*
  Autor: Marco Antonio Maldonado Orozco
*/
//Esta función agrega un nuevo input de forma genérica del tipo que se le pase y lo añade en el contenedor correspondiente
//a esos inputs teniendo en cuenta también el linkId del item que sería el inputId
function agregarOtroInput(inputId, inputType) {
	var container = document.getElementById(inputId + '~' + inputType + '-container');
    var newIndex = container.querySelectorAll('input[type="' + inputType + '"][name*="' + inputId + '"]').length + 1;

	var input = document.createElement('input');
	input.type = inputType;
	input.name = inputId + '~' + inputType + '-' + newIndex; // Nombre único con el linkId y un índice
    input.id = inputId + '~' + inputType + '-' + newIndex;
	container.appendChild(input);
}

//Esta función agrega un nuevo input de tipo url para referencias cuando se aprueta al botón, debe tener función propia
function agregarOtraReferencia(inputId) {
	var container = document.getElementById(inputId + '~reference-container');
	var newIndex = container.querySelectorAll('input[type="url"][name*="' + inputId + '"]').length + 1;
	var input = document.createElement('input');
	input.type = "url";
	input.name = inputId + '~reference-' + newIndex; // Nombre único con el linkId y un índice
    input.id = inputId + '~reference-' + newIndex;
	input.placeholder = "Introduce una referencia FHIR"
	container.appendChild(input);
}

//Esta función maneja que cuando se seleccione un radio los inputs de texto asociado se limpien
 function handleRadioChange(radio) {
    // Obtiene los input de texto asociado a la pregunta coding
	var textInputs = document.querySelectorAll('input[type="text"][id="' + radio.id + '~text"]');

    // Establece el contenido de todos los inputs de texto asociados en blanco si se selecciona un radio
    if (textInputs != null) {
        if (radio.checked) {
            textInputs.forEach(function (textInput) {
                textInput.value = '';
            });
        }
    }
}

//Maneja el clic en los select cuando hay más de tres opciones para que elimine el valor de los inputs de texto asociados
 function handleSelectChange(select) {
    // Obtiene los input de texto asociados a la pregunta coding
    var textInputs = document.querySelectorAll('input[type="text"][id="' + select.id + '~text"]');

    // Establece el contenido de todos los inputs de texto asociados en blanco si se selecciona un valor en el select
    if (textInputs != null) {
        textInputs.forEach(function (textInput) {
            textInput.value = '';
        });
    }
}

// Maneja el clic en el campo de texto de las opciones extras para que deseleccione los radios/select asociados
function handleTextInputClick(name) {
    // Desseleccionar todas las opciones de radio cuando se hace clic en un input de texto
    var radioButtons = document.querySelectorAll('input[type="radio"][id="' + name + '"]');
    radioButtons.forEach(function (radio) {
        radio.checked = false;
    });
    
    //Esto para obtener el select correspondiente para eliminar el valor cuando se haga clic en una opción extra
    var select = document.querySelector('select[id="' + name + '"]');
    // Establece el valor del select en blanco si se hace clic en una opción extra
    if (select != null) {
        select.value = '';
    }
}

//Esta función agrega un nuevo input de tipo textarea cuando se aprueta al botón, necesita función propia porque no es un input como tal
function agregarOtroTextarea(inputId) {
	var container = document.getElementById(inputId + '~textarea-container');
	var newIndex = container.querySelectorAll('input[type="textarea"][name*="' + inputId + '"]').length + 1;
	var textarea = document.createElement('textarea');
	textarea.name = inputId + '~textarea-' + newIndex; // Nombre único con el linkId y un índice
    textarea.id = inputId + '~textarea-' + newIndex;
	container.appendChild(textarea);
}

//Esta función agrega un nuevo input de tipo number, necesita función propia porque calcula el step 
//según el tipo que sea cuando se aprieta al botón
function agregarOtroInputNumber(inputId, type) {
	var container = document.getElementById(inputId + '~number-container');
	var newIndex = container.querySelectorAll('input[type="number"][name*="' + inputId + '"]').length + 1;
	var input = document.createElement('input');
	input.type = "number";
	input.name = inputId + '~' + type + '-' + newIndex; // Nombre único con el linkId y un índice
    input.id = inputId + '~' + type + '-' + newIndex;
	input.step = type == 'QUANTITY' ? "0.1" : type == 'DECIMAL' ? "0.01" : "1";
	container.appendChild(input);
}