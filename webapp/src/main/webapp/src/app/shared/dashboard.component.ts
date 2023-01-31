import {Component, ElementRef, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {DashboardService} from "./dashboard.service";
import {Dashboard} from "./dashboard";
import {Subscription, timer} from 'rxjs';
import {select, Store} from "@ngrx/store";
import {GET_PROPS} from "../state/app.reducer";
import {SyncMode} from "../receiver/shared/sync-mode.enum";
import {LoadDashboard} from "./state/dashboard.actions";
import {GET_DASHBOARD, GET_DASHBOARD_ERROR} from "./state/dashboard.reducer";
import {HttpErrorResponse} from "@angular/common/http";
import {NgbModal, NgbModalOptions, NgbModalRef} from "@ng-bootstrap/ng-bootstrap";

@Component({template: ''})
export abstract class DashboardComponent implements OnInit, OnDestroy {

	dashboard?: Dashboard;

	reloadTimer?: Subscription;

	propsLoaded?: Subscription;

	dashboardLoaded?: Subscription;

	dashboardError?: Subscription;

	modalRef?: NgbModalRef;

	@ViewChild('serverDownMsgTemplate')
	serverDownTemplate?: ElementRef;

	constructor(private service: DashboardService, private store: Store, private modalService: NgbModal) {
	}

	ngOnInit(): void {
		this.propsLoaded = this.store.pipe(select(GET_PROPS)).subscribe(props => {
			if (props.syncMode == this.getSyncMode()) {
				this.dashboardLoaded = this.store.pipe(select(GET_DASHBOARD)).subscribe(dashboard => {
					this.dashboard = dashboard;
				});

				this.dashboardError = this.store.pipe(select(GET_DASHBOARD_ERROR)).subscribe(error => {
					this.handleLoadError(error);
				});

				this.reloadTimer = timer(0, 30000).subscribe(() => {
					this.store.dispatch(new LoadDashboard());
				});
			}
		});
	}

	handleLoadError(error: HttpErrorResponse): void {
		if (error) {
			if (error.status === 0) {
				this.stopSubscriptions();
				this.showErrorDialog();
			} else {
				throw error;
			}
		}
	}

	showErrorDialog(): void {
		const dialogConfig: NgbModalOptions = {
			size: 'lg',
			backdrop: 'static'
		}

		this.modalRef = this.modalService.open(this.serverDownTemplate, dialogConfig);
	}

	ngOnDestroy(): void {
		this.stopSubscriptions();
	}

	reload(): void {
		this.modalRef?.close();
		window.location.href = "/";
	}

	stopSubscriptions(): void {
		this.reloadTimer?.unsubscribe();
		this.propsLoaded?.unsubscribe();
		this.dashboardLoaded?.unsubscribe();
		this.dashboardError?.unsubscribe();
	}

	abstract getSyncMode(): SyncMode;

}
