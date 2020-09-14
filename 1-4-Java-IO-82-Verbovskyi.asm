.386
.model flat,stdcall
option casemap:none

include     E:\masm32\include\windows.inc
include     E:\masm32\include\kernel32.inc
include     E:\masm32\include\masm32.inc
includelib  E:\masm32\lib\kernel32.lib
includelib  E:\masm32\lib\masm32.lib

_NumbToStr   PROTO :DWORD,:DWORD
_main        PROTO

main	PROTO
second	PROTO

.data
buff        db 11 dup(?)

.code
_start:
	invoke  _main
	invoke  _NumbToStr, ebx, ADDR buff
	invoke  StdOut,eax
	invoke  ExitProcess,0

_main PROC

	call main
	call second

	ret

_main ENDP

main PROC
mov ebx, 258
ret
main ENDP
second PROC
mov ebx, 12
ret
second ENDP

_NumbToStr PROC uses ebx x:DWORD,buffer:DWORD

	mov     ecx,buffer
	mov     eax,x
	mov     ebx,10
	add     ecx,ebx
@@:
	xor     edx,edx
	div     ebx
	add     edx,48
	mov     BYTE PTR [ecx],dl
	dec     ecx
	test    eax,eax
	jnz     @b
	inc     ecx
	mov     eax,ecx
	ret

_NumbToStr ENDP

END _start
