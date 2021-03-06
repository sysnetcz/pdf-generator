# Služba PDF (pdf-service)

## Popis
Projekt má tyto cíle:

1.  Umožnit správu šablon Jaspersoft Studio pro tvrobu PDF výstupů
2.  Poskytovat veřejné REST API pro tvorbu PDF dokumentů naplněním dat do šablon Jaspersoft Studio 

### REST API
> 
Endpoint:	<https://service.cites.cz/pdf-service/api>   
WADL:		<https://service.cites.cz/pdf-service/api/application.wadl>   

#### Zdroje


*   **template**  
metoda **GET** - vrací seznam instalovaných šablonu   
metoda **POST** - uploaduje šablonu nebo zdroj šablony. Vstupní data jsou JSON:   
`{"key": "PR",  "fileName": "nazev_souboru.jrxml",   "file": "Base64 obsah souboru" }`   
  
 
*   **template/{key}**   
metoda **GET** - vrací metadata šablony identifikované klíčem **{key}**
  
   
*   **template/{key}/pdf**   
metoda **GET** - vrací seznam PDF souborů vygenerovaných pomocí šablony identifikované klíčem **{key}**  
metoda **POST** - uploaduje JSON data a vygeneruje PDF dokument pomocí šablony identifikované klíčem **{key}**. Vrací metadata PDF souboru.   
   
  
*   **template/{key}/pdf{id}** metoda **GET** - downloaduje PDF soubor vygenerovaný pomocí šablony identifikované klíčem **{key}** pod identifikátorem **{id}**  

  
*   **verify**
metoda **GET** - vrací textovou informaci, že služba běží  

### Webové rozhraní
Poskytuje základní informace o běhu služby a umožňuje ruční upload šablon.

## Shrnutí
Služba je nezávislá na ostatních projektech a může složit jako obecný generátor PDF dokumentů a/nebo reportů. 
Pro tvormu šablon se používá OpenSource produkt **Jaspersoft Studio** viz [Vytváření vlastní šablony pomocí aplikace Jaspersoft Studio](https://community.jaspersoft.com/wiki/creating-custom-template-jaspersoft-studio "Jaspersoft Studio").

## Spolupráce
Používá JAR knihovnu vytvořenou projektem **pdf-generator** <http://git.sysnet.cz/cites/pdf-generator.git> 

## Docker
Služba je plne dockerizovaná obsahuje jak __Dockerfile__, tak i __docker-compose.yml__. 
Obraz docker se postaví a spustí tímto příkazem:

	$ docker build --tag sysnetcz/pdf .
	$ docker run -d -p 127.0.0.1:8081:8080 --name pdf -t sysnetcz/pdf


### docker-compose
	
Stack docker-compose se postaví a spustí takto:

	$ docker-compose build
	$ docker-compose up -d 	

### Systémové proměnné

Aplikace může používat systémopvé proměnné vzorového obrazu __tomcat:5.8__ a má jednu vlastní proměnnou __PDF_DATA_DIR__ pro umístění datového adresáře v obraze. Obvykle ji není nutno používat.

### Persistence dat

Je vhodné umístit data vně kontejneru. 

	$ docker volume create pdf_data
	$ docker run -d \
		-p 127.0.0.1:8081:8080 \
		--name pdf \
		--restart=always \
		-v pdf_data:/usr/local/data
		-t sysnetcz/pdf
 