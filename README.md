# POD TPE1 - GRUPO 6 - Reservas de Atracciones de Parques Temáticos
Implementación de  un sistema remoto thread-safe para la reserva de atracciones de un parque temático en un año, permitiendo notificar a los usuarios del servicio y ofreciendo
reportes de las reservas realizadas al momento.

## Instrucciones de Compilación
Para compilar el proyecto, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
chmod +x compile.sh
./compile.sh
```
Este script se encargará de compilar el proyecto con `maven`, generando los archivos `.tar.gz` correspondientes en un directorio temporal `/tmp` en el cual se encontrarán los archivos disponibles para su ejecución. 

## Instrucciones de Ejecución
### Servidor
Para ejecutar el servidor, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
cd ./tmp/tpe1-g6-server-2023.2Q/
./run-server
```
El servidor por defecto corre en el puerto `50055` 

### Cliente de Administración del Parque
Para ejecutar el cliente de administración del parque, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
cd ./tmp/tpe1-g6-client-2023.2Q/
./admin-cli -DserverAddress=xx.xx.xx.xx:50055 -Daction=actionName [ -DinPath=filename | -Dride=rideName | -Dday=dayOfYear | -Dcapacity=amount ]
````


### Cliente de Reserva de atracciones
Para ejecutar el cliente de reserva de atracciones, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
cd ./tmp/tpe1-g6-client-2023.2Q/
./book-cli -DserverAddress=xx.xx.xx.xx:50055 -Daction=actionName [ -Dday=dayOfYear -Dattraction=rideName -Dvisitor=visitorId -Dslot=bookingSlot -DslotTo=bookingSlotTo ]
```


### Cliente de Notificaciones de una atracción
Para ejecutar el cliente de notificaciones de una atracción, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
cd ./tmp/tpe1-g6-client-2023.2Q/
./notif-cli -DserverAddress=xx.xx.xx.xx:50055 -Daction=actionName -Dday=dayOfYear -Dride=rideName -Dvisitor=visitorId
```


### Cliente de Consulta
Para ejecutar el cliente de consulta, se deben ejecutar los siguientes comandos en la carpeta raíz del proyecto:
```bash
cd ./tmp/tpe1-g6-client-2023.2Q/
./query-cli -DserverAddress=xx.xx.xx.xx:50055 -Daction=actionName -DoutPath=query.txt
```



## Integrantes
| Nombre                 | Legajo |
|------------------------|--------|
| Ortu, Agustina Sol     | 61548  |
| Vasquez Currie, Malena | 60072  |
| Mattiussi, Agustín     | 61361  |
| Sasso, Julián Martín   | 61535  |
