/*
 * functions.c
 *
 *  Created on: 23 сент. 2016 г.
 *      Author: Тимур
 */
#include "stm32f4xx_hal.h"
#include "fatfs.h"
#include "usb_device.h"
#include "functions.h"


extern SD_HandleTypeDef hsd;
extern HAL_SD_CardInfoTypedef SDCardInfo;

extern UART_HandleTypeDef huart2;
extern UART_HandleTypeDef huart3;

char DirName[LOCAL_BUF_SIZE];
char SubDirName[LOCAL_BUF_SIZE];
char FileName[LOCAL_BUF_SIZE];
unsigned char Buf[GLOBAL_BUFFER_SIZE];
unsigned char writeBuffer[GLOBAL_BUFFER_SIZE];

enum state state_t;
enum path_state path_state_t;

FRESULT res,fileRes;
FIL fsrc, fdst,MyFile;     /* File object */
FATFS SDFatFs;  /* File system object for SD card logical drive */
char SDPath[4]; /* SD card logical drive path */

volatile unsigned char UART_Data;
uint8_t C_UART_RxFlag = 0;
UINT br, bw;
volatile  unsigned char recieve;
static UINT count_buf = 0;

//#define HAL_UART_MODULE_ENABLED
//==============================================================================
void UART_RX(void)
{
	if(C_UART_RxFlag==1)
	{
		C_UART_RxFlag = 0;

		//while(!(USART2->SR & USART_SR_TXE));
			//	USART2->DR = UART_Data;
		HAL_UART_Receive_IT(&huart3,&UART_Data,1);
		//HAL_UART_Receive(&huart3,&UART_Data,1, 0xFFFF);
		readRXbuffer();
	}

}
//==============================================================================
/*void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  if(huart->Instance == USART3)
  {
	  C_UART_RxFlag = 1;
	  UART_RX();
  }
}*/
//==============================================================================
void ControlNull()
{

   if(strstr(Buf,"PacketPath"))
     {
        state_t = control_packet_path;
        USBD_Stop(&hUsbDeviceFS);
     }
   if(strstr(Buf,"FileTransfer"))
     {
        state_t = control_packet_file;
     }
}
//==============================================================================
void createDir()
{
	res = FR_OK;
	res = f_mkdir (DirName);
		if(res==FR_OK)
				  {
					res=f_chdir(DirName);
					if(res==FR_OK)
					 {
						 HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_SET);
					 }
					 else
					 {
						 HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_RESET);
						 UART2_Write_Text("Error in SubDirName");
						 Error_Handler();
					 }
				  }
		if(res==FR_EXIST)
		  {
			res=f_chdir(DirName);
					if(res==FR_OK)
					 {
						 HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_SET);
					 }
					 else
					 {
						 HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_RESET);
						 UART2_Write_Text("Error in SubDirName");
						 Error_Handler();
					 }
		  }

}
  //Здесь далее пишу любую другую операцию
  //FATFS_UnLinkDriver(SD_Path);
//==============================================================================
void createSubDir()
{
	res = FR_OK;
	res = f_mkdir (SubDirName);
	if(res==FR_OK)
			  {
				res=f_chdir(SubDirName);
				if(res==FR_OK)
				 {
					 HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_SET);
				 }
				 else
				 {
					 HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_RESET);
					 UART2_Write_Text("Error in SubDirName");
					 Error_Handler();
				 }
			  }
	if(res==FR_EXIST)
	  {
		res=f_chdir(SubDirName);
				if(res==FR_OK)
				 {
					 HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_SET);
				 }
				 else
				 {
					 HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_RESET);
					 UART2_Write_Text("Error in SubDirName");
					 Error_Handler();
				 }
	  }
}
//==============================================================================
void openFile()
{
 res = f_open(&fdst, FileName, FA_CREATE_ALWAYS | FA_WRITE);
 if(res==FR_OK)
 {
	 HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_SET);
 }
 else
 {
	 HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_RESET);
	 UART2_Write_Text("Error open file");
	 Error_Handler();
 }


}
//==============================================================================
void decoder(char* Buf,const unsigned char len)
{
 //static  unsigned char i = 0;
 char* temp_buf = 0;
 char *res = 0;
 static char str_len = 0;
 static char count = 0;
 //static char sub_dir_created= 0;
 //-----------------------------------------------------------------------------
      temp_buf = Buf;
      resetBuffer(DirName,LOCAL_BUF_SIZE);
      resetBuffer(SubDirName,LOCAL_BUF_SIZE);
      resetBuffer(FileName,LOCAL_BUF_SIZE);
//------------------------------------------------------------------------------
      res = strstr(temp_buf, "DirName");
      str_len = strlen("DirName");
      res+=str_len;
      count = 0;
      while((*res)!=STAR)
       {
         DirName[count++]  = (*res);
         res++;
       }
//------------------------------------------------------------------------------
      res = strstr(temp_buf, "SubDirName");
      str_len = strlen("SubDirName");
      res+=str_len;
      count = 0;
       while((*res)!=STAR)
       {
         SubDirName[count++] = (*res);
         res++;
       }
//------------------------------------------------------------------------------
      res = strstr(temp_buf, "FileName");
      str_len = strlen("FileName");
      res+=str_len;
      count = 0;
       while((*res)!=STAR)
       {
         FileName[count++] = (*res);
         res++;
       }
 }
//==============================================================================
char seek(unsigned char* Buf, unsigned char* Text,char lenword,unsigned int array_size)
{
  unsigned char* pBuf   = Buf;
  unsigned char* pText  = Text;
  static char size_b = 0;
  unsigned int size = 0;
  for(size=0;size<array_size;size++)
   {
     if((*pBuf)==(*pText))
       {
         pText++;
         size_b++;
         if(size_b>=lenword)
          {
            return 1;
          }
       }
        else
         {
           if(size_b>0)
            {
              pText--;
            }
           size_b = 0;

         }
     pBuf++;

   }
  return 0;
}
//==============================================================================
void Disk_Init()
{
	if( disk_initialize(0)==RES_OK)
	  {
		res = f_mount(&SDFatFs, (TCHAR const*)SDPath, 1);
		if( res!= FR_OK)
		  	      {
		  	         //FatFs Initialization Error
				   UART2_Write_Text("Error mount disk");
				   Error_Handler();

		  	      //HAL_GPIO_WritePin(GPIOD, LD4_Pin, GPIO_PIN_RESET);
		  	      }
				  else
				  {
					  //HAL_GPIO_WritePin(GPIOD, LD4_Pin, GPIO_PIN_SET);
				  }
	  }
}
//==============================================================================
void stateMashin()
{
   switch(state_t)
      {
//------------------------------------------------------------------------------
        case null:
             ControlNull();
        break;
//------------------------------------------------------------------------------
        case control_packet_path:
             controlPacketPath();
        break;
//------------------------------------------------------------------------------
        case path_packet_full:
             decoder(Buf,GLOBAL_BUFFER_SIZE);
             state_t = create_path;
             path_state_t = dir;
        break;
//------------------------------------------------------------------------------
        case create_path:
             createPath();
        break;
//------------------------------------------------------------------------------
        case control_packet_file:
             controlPacketFile();
        break;
//------------------------------------------------------------------------------
        case file_packet_full:
             readFilePacket();
        break;
//------------------------------------------------------------------------------
        case end_file_packet:
             readFilePacketEnd();
        break;

      }
}
//==============================================================================
void resetBuffer(unsigned char* Buf,unsigned int len )
{
  unsigned int i = 0;
  for(i = 0;i < len;i++)
   {
      Buf[i] = 0;
   }
}
//==============================================================================
void controlPacketFile()
{
  if(seek(Buf,"End",3,GLOBAL_BUFFER_SIZE))
    {
       state_t = file_packet_full;
    }
  if(seek(Buf,"Stop",4,GLOBAL_BUFFER_SIZE))
    {
       state_t =  end_file_packet;
    }
}
//==============================================================================
void readFilePacket()
{
 unsigned int str_len = 0,length = 0;
 unsigned char* res;
 unsigned int size_array = 0;

  resetBuffer(writeBuffer,GLOBAL_BUFFER_SIZE);
  //resetBuffer(readBuffer,GLOBAL_BUFFER_SIZE);
  res = strstr(Buf, "FileTransfer");
  str_len = strlen("FileTransfer");
  res+=str_len;
     while(TRUE)
      {
         if(seek(writeBuffer,"End",3,GLOBAL_BUFFER_SIZE))
          {
            break;
          }
          else
           {
             writeBuffer[length] = *res;
             length++;
             res++;
           }
      }
  size_array = length -  strlen("End");
  //uint8_t * buffer = (uint8_t*) malloc(size_array);
  //memcpy (buffer, writeBuffer,size_array);

  fileRes = FR_OK;
  fileRes = f_write(&fdst, writeBuffer, size_array, &bw);
  if(fileRes!=FR_OK)
  {
	  UART2_Write_Text("Error write file");
	  Error_Handler();
  }
  //free(buffer);
  clearBuf();
  UART3_Write_Text("ACK");
  state_t = null;
}
//==============================================================================
void send_to_uart(uint8_t data)  {
while(!(USART3->SR & USART_SR_TC));
USART3->DR=data;
}
//==============================================================================
void UART3_Write_Text(char * text)
{
	uint8_t i = 0;
	while(text[i])
	{
		send_to_uart(text[i]);
		i++;
		//HAL_Delay(100);
	}
	//send_to_uart('\0');
}
//==============================================================================
void UART2_Write_Text(char * text)
{
	uint8_t i = 0;
	while(text[i])
	{
		while(!(USART2->SR & USART_SR_TC));
		USART2->DR=text[i];
		i++;

	}
	send_to_uart('\r');
	send_to_uart('\n');
}
//==============================================================================
void readFilePacketEnd()
{
 unsigned int str_len = 0,length = 0;
 unsigned char* res;
 unsigned int size_array = 0;

  resetBuffer(writeBuffer,GLOBAL_BUFFER_SIZE);
  res = strstr(Buf, "FileTransfer");
  str_len = strlen("FileTransfer");
  res+=str_len;
     while(TRUE)
      {
         if(seek(writeBuffer,"Stop",4,GLOBAL_BUFFER_SIZE))
          {
            break;
          }
          else
           {
             writeBuffer[length] = *res;
             length++;
             res++;
           }
      }
  size_array = length -  strlen("Stop");
  //uint8_t * buffer = (uint8_t*) malloc(size_array);
  //memcpy (buffer, writeBuffer,size_array+1);
  fileRes = FR_OK;
  fileRes = f_write(&fdst, writeBuffer, size_array, &bw);
  if(fileRes!=FR_OK)
    {
	  UART2_Write_Text("Error last write file");
	  Error_Handler();
    }

  fileRes = f_close(&fdst);
  if(fileRes!=FR_OK)
  {
	   UART2_Write_Text("Error close file");
	  Error_Handler();
  }
  //free(buffer);
  clearBuf();
  fileRes=FR_OK;
  fileRes = f_chdir("..");
  if(fileRes==FR_OK)
	  {
	  fileRes = f_chdir("..");
	  if(fileRes==FR_OK)
			  {

			  }
		  else
		  {
			   UART2_Write_Text("Error go to root dir");
			  Error_Handler();
		  }
	  }
	  else
	  {
		  UART2_Write_Text("Error go to root dir");
		  Error_Handler();
	  }

  clearBuf();
  resetLeds();
  UART3_Write_Text("ACK");
  //UART3_Write_Text("ACK");
  state_t = null;

  //SystemReset();
}
//==============================================================================
void resetLeds()
{
	HAL_GPIO_WritePin(GPIOD, LD3_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOD, LD4_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOD, LD5_Pin, GPIO_PIN_RESET);
	HAL_GPIO_WritePin(GPIOD, LD6_Pin, GPIO_PIN_RESET);
}
//==============================================================================
void controlPacketPath()
{
  if(strstr(Buf,"PacketPathEnd"))
     {
       state_t = path_packet_full;
     }
}
//==============================================================================
void readRXbuffer()
{
 Buf[count_buf] =  UART_Data;
   count_buf++;
    if(count_buf>=(GLOBAL_BUFFER_SIZE-1))
      {
       clearBuf();
      }
}
//==============================================================================
void control_in_buf()
 {
     if(seek(Buf,"RST",3,GLOBAL_BUFFER_SIZE))
     {
    	 NVIC_SystemReset();
     }
    if(seek(Buf,"CONNECT",7,GLOBAL_BUFFER_SIZE))
     {
       clearBuf();
     }

 }
//==============================================================================
void clearBuf()
{
   unsigned int i = 0;
   count_buf = 0;
   for(i = 0;i<GLOBAL_BUFFER_SIZE;i++)
    {
      Buf[i] = 0;
    }
}
//==============================================================================
void createPath()
{
 switch(path_state_t)
 {
    case dir:
         createDir();
         path_state_t = sub_dir;
    break;

    case sub_dir:
         createSubDir();
         path_state_t = open_file;
    break;

    case open_file:
         openFile();
         clearBuf();
         state_t = null;
    break;
 }
}
//==============================================================================

void InitStateMashine()
{
   state_t = null;
}
//==============================================================================
