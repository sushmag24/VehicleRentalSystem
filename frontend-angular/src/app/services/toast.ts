import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ToastData {
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  show: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastState = new BehaviorSubject<ToastData>({
    message: '',
    type: 'success',
    show: false
  });

  toast$ = this.toastState.asObservable();
  private timer: any;

  show(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'success'): void {
    if (this.timer) {
      clearTimeout(this.timer);
    }
    this.toastState.next({ message, type, show: true });
    this.timer = setTimeout(() => {
      this.toastState.next({ ...this.toastState.value, show: false });
    }, 3500);
  }
}
