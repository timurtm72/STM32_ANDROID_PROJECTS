/*
 * functions.h
 *
 *  Created on: 23 сент. 2016 г.
 *      Author: “имур
 */
#include "stm32f4xx_hal.h"
#include "fatfs.h"
#include "usb_device.h"

#ifndef FUNCTIONS_H_
#define FUNCTIONS_H_

#define  LOCAL_BUF_SIZE         30
#define  GLOBAL_BUFFER_SIZE     1048
#define  STAR                   42
#define  TRUE                   1
#define  FALSE                  0
#define  ON                     1
#define  OFF                    0
#define  SET                    1
#define  RESET                  0

extern volatile unsigned char UART_Data;
extern uint8_t C_UART_RxFlag;

extern char DirName[LOCAL_BUF_SIZE];
extern char SubDirName[LOCAL_BUF_SIZE];
extern char FileName[LOCAL_BUF_SIZE];
extern enum state state_t;
extern enum path_state path_state_t;

enum state { null=0,path_packet_full,create_path,end_file_packet,file_packet_full,
control_packet_path,control_packet_file } ;
enum path_state { dir=0,sub_dir,open_file };

//=================================================================================
void UART_RX();
void ControlNull();
void createDir();
void createSubDir();
void openFile();
void decoder(char* Buf,const unsigned char len);
char seek(unsigned char* Buf, unsigned char* Text,char lenword,unsigned int array_size);
void Disk_Init();
void stateMashin();
void resetBuffer(unsigned char* Buf,unsigned int len );
void controlPacketFile();
void readFilePacket();
void readFilePacketEnd();
void resetLeds();
void controlPacketPath();
void readRXbuffer();
void control_in_buf();
void clearBuf();
void createPath();
void UART3_Write_Text(char * text);
void InitStateMashine();
void send_to_uart(uint8_t data);
void UART2_Write_Text(char * text);


//=================================================================================
#endif /* FUNCTIONS_H_ */
