#!/bin/bash

# InvoiceMe Development Environment Startup Script
# Manages database, backend, and frontend processes

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# PID file to track processes
PID_FILE=".dev-pids"

# Function to print colored messages
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Function to start all services
start_all() {
    log_info "Starting InvoiceMe development environment..."

    # Start PostgreSQL
    log_info "Starting PostgreSQL database..."
    docker compose up -d
    if [ $? -eq 0 ]; then
        log_success "PostgreSQL started"
    else
        log_error "Failed to start PostgreSQL"
        exit 1
    fi

    # Wait for database to be ready
    log_info "Waiting for database to be ready..."
    sleep 3

    # Start Backend
    log_info "Starting Spring Boot backend on port 8080..."
    mvn spring-boot:run > backend.log 2>&1 &
    BACKEND_PID=$!
    echo "BACKEND_PID=$BACKEND_PID" > $PID_FILE
    log_success "Backend started (PID: $BACKEND_PID)"
    log_info "Backend logs: tail -f backend.log"

    # Wait for backend to start
    log_info "Waiting for backend to start..."
    sleep 10

    # Start Frontend
    log_info "Starting Vite frontend on port 5173..."
    cd invoice-me-frontend
    npm run dev > ../frontend.log 2>&1 &
    FRONTEND_PID=$!
    cd ..
    echo "FRONTEND_PID=$FRONTEND_PID" >> $PID_FILE
    log_success "Frontend started (PID: $FRONTEND_PID)"
    log_info "Frontend logs: tail -f frontend.log"

    echo ""
    log_success "All services started!"
    echo ""
    echo "  🗄️  Database:  http://localhost:5432 (PostgreSQL)"
    echo "  🚀 Backend:   http://localhost:8080"
    echo "  🎨 Frontend:  http://localhost:5173"
    echo ""
    log_info "To stop all services, run: ./dev.sh stop"
    log_info "To view logs: tail -f backend.log frontend.log"
}

# Function to stop all services
stop_all() {
    log_info "Stopping InvoiceMe development environment..."

    if [ ! -f $PID_FILE ]; then
        log_warn "No PID file found. Services may not be running."
    else
        # Read PIDs from file
        source $PID_FILE

        # Stop Frontend
        if [ ! -z "$FRONTEND_PID" ]; then
            log_info "Stopping frontend (PID: $FRONTEND_PID)..."
            kill $FRONTEND_PID 2>/dev/null || log_warn "Frontend process not found"
        fi

        # Stop Backend
        if [ ! -z "$BACKEND_PID" ]; then
            log_info "Stopping backend (PID: $BACKEND_PID)..."
            kill $BACKEND_PID 2>/dev/null || log_warn "Backend process not found"
        fi

        rm $PID_FILE
    fi

    # Stop PostgreSQL
    log_info "Stopping PostgreSQL database..."
    docker compose down

    log_success "All services stopped"
}

# Function to show status
status() {
    log_info "InvoiceMe Development Environment Status"
    echo ""

    # Check Docker
    if docker ps | grep -q invoiceme-postgres; then
        log_success "PostgreSQL: Running"
    else
        log_error "PostgreSQL: Not running"
    fi

    # Check Backend
    if [ -f $PID_FILE ]; then
        source $PID_FILE
        if ps -p $BACKEND_PID > /dev/null 2>&1; then
            log_success "Backend: Running (PID: $BACKEND_PID)"
        else
            log_error "Backend: Not running"
        fi

        if ps -p $FRONTEND_PID > /dev/null 2>&1; then
            log_success "Frontend: Running (PID: $FRONTEND_PID)"
        else
            log_error "Frontend: Not running"
        fi
    else
        log_error "Backend: Not running"
        log_error "Frontend: Not running"
    fi
}

# Function to restart all services
restart() {
    stop_all
    sleep 2
    start_all
}

# Main script
case "$1" in
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        echo ""
        echo "Commands:"
        echo "  start    - Start all services (database, backend, frontend)"
        echo "  stop     - Stop all services"
        echo "  restart  - Restart all services"
        echo "  status   - Check status of all services"
        exit 1
        ;;
esac

exit 0
